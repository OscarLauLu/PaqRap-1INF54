import React, { useEffect, useRef, useState } from 'react';
import { FileUp, Info, PenLine, Construction } from 'lucide-react';
import { bloqueosApi } from '../../api/bloqueosApi';
import type { BloqueoVial } from '../../types';
import type { NuevoBloqueoDatos } from '../../types/registro';
import { derivarEstadoBloqueo } from '../../types/registro';
import { Card } from '../ui/Card';
import { Badge } from '../ui/Badge';
import { inputBase } from './estilos';
import { Aviso, Campo } from './ui';
import { formatFechaHora } from './utils';

interface Errores {
  inicio?: string;
  fin?: string;
  coordenadas?: string;
}

const ESTADO_TONO = { Programado: 'azul', Activo: 'rojo', Vencido: 'gris' } as const;

export const PestanaBloqueos: React.FC = () => {
  const [bloqueos, setBloqueos] = useState<BloqueoVial[]>([]);
  const [cargando, setCargando] = useState(true);

  // --- Flujo 1: subir archivo mensual de bloqueos ---
  const [archivo, setArchivo] = useState<File | null>(null);
  const [subiendo, setSubiendo] = useState(false);
  const inputArchivo = useRef<HTMLInputElement>(null);

  // --- Flujo 2: registro manual de un bloqueo individual ---
  const [inicio, setInicio] = useState('');
  const [fin, setFin] = useState('');
  const [coordenadasNodos, setCoordenadasNodos] = useState('');
  const [errores, setErrores] = useState<Errores>({});
  const [registrando, setRegistrando] = useState(false);

  const [mensaje, setMensaje] = useState<string | null>(null);

  const cargarBloqueos = () => {
    setCargando(true);
    bloqueosApi
      .listar()
      .then(setBloqueos)
      .catch(() => setBloqueos([]))
      .finally(() => setCargando(false));
  };

  useEffect(cargarBloqueos, []);

  const abrirSelector = () => inputArchivo.current?.click();

  const handleArchivo = (e: React.ChangeEvent<HTMLInputElement>) => {
    const elegido = e.target.files?.[0];
    if (elegido) {
      setArchivo(elegido);
      setMensaje(null);
    }
    e.target.value = '';
  };

  const subirArchivo = async () => {
    if (!archivo) return;
    setSubiendo(true);
    try {
      const cargados = await bloqueosApi.subirArchivo(archivo);
      setMensaje(`Archivo "${archivo.name}" procesado: ${cargados.length} bloqueo(s) registrados en el servidor.`);
      setArchivo(null);
      cargarBloqueos();
    } catch {
      setMensaje('No se pudo procesar el archivo. Revisa el formato (##d##h##m-##d##h##m:x1,y1,...,xn,yn).');
    } finally {
      setSubiendo(false);
    }
  };

  const registrarManual = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const nuevos: Errores = {};
    if (!inicio) nuevos.inicio = 'Elige la fecha y hora de inicio';
    if (!fin) nuevos.fin = 'Elige la fecha y hora de fin';
    if (fin && inicio && new Date(fin) <= new Date(inicio)) nuevos.fin = 'Debe ser posterior al inicio';
    if (!coordenadasNodos.trim()) nuevos.coordenadas = 'Ingresa las coordenadas de los tramos (x1,y1,x2,y2,...)';

    setErrores(nuevos);
    if (Object.keys(nuevos).length > 0) return;

    setRegistrando(true);
    try {
      const datos: NuevoBloqueoDatos = {
        inicio: new Date(inicio).toISOString(),
        fin: new Date(fin).toISOString(),
        coordenadasNodos: coordenadasNodos.trim(),
      };
      const registrado = await bloqueosApi.registrarManual(datos);
      setMensaje(`Bloqueo ${registrado.codigo} registrado manualmente.`);
      setInicio('');
      setFin('');
      setCoordenadasNodos('');
      cargarBloqueos();
    } catch {
      setMensaje('No se pudo registrar el bloqueo. Verifica los datos e inténtalo de nuevo.');
    } finally {
      setRegistrando(false);
    }
  };

  return (
    <>
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card icono={FileUp} titulo="Subir archivo de bloqueos">
          <input ref={inputArchivo} type="file" accept=".txt,.csv" onChange={handleArchivo} className="hidden" />
          <div className="flex flex-col items-center gap-4">
            <button
              type="button"
              onClick={abrirSelector}
              className="w-full h-32 rounded-xl border-2 border-dashed border-(--color-brand-500) bg-(--color-brand-50) hover:bg-(--color-brand-100) transition-colors flex flex-col items-center justify-center gap-1"
            >
              <FileUp className="w-8 h-8 text-(--color-brand-500)" />
              <span className="font-bold text-(--color-brand-700) text-sm">
                {archivo ? archivo.name : 'Selecciona el archivo mensual'}
              </span>
              <span className="text-xs text-(--color-ink-400)">formato aaaamm.bloqueadas</span>
            </button>
            <div className="flex items-start gap-2 rounded-lg bg-(--color-brand-50) px-4 py-3 text-xs text-(--color-ink-500) w-full">
              <Info className="w-4 h-4 text-(--color-brand-500) shrink-0 mt-0.5" />
              <p>Registra en bloque todos los bloqueos planificados por la municipalidad para el mes (RF-11).</p>
            </div>
            <button
              type="button"
              onClick={subirArchivo}
              disabled={!archivo || subiendo}
              className="w-full rounded-xl bg-(--color-brand-500) hover:bg-(--color-brand-600) disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold px-6 py-2.5 text-sm transition-colors"
            >
              {subiendo ? 'Procesando...' : 'Procesar archivo'}
            </button>
          </div>
        </Card>

        <Card icono={PenLine} titulo="Registro manual de un bloqueo">
          <form onSubmit={registrarManual} noValidate className="space-y-4">
            <Campo etiqueta="Inicio de bloqueo" error={errores.inicio}>
              <input type="datetime-local" value={inicio} onChange={(e) => setInicio(e.target.value)} className={inputBase} />
            </Campo>
            <Campo etiqueta="Fin de bloqueo" error={errores.fin}>
              <input type="datetime-local" value={fin} onChange={(e) => setFin(e.target.value)} className={inputBase} />
            </Campo>
            <Campo etiqueta="Ubicación (tramos)" error={errores.coordenadas}>
              <input
                type="text"
                value={coordenadasNodos}
                onChange={(e) => setCoordenadasNodos(e.target.value)}
                placeholder="31,21,34,21"
                className={inputBase}
              />
            </Campo>
            <button
              type="submit"
              disabled={registrando}
              className="w-full rounded-xl bg-gray-700 hover:bg-gray-800 disabled:opacity-50 text-white font-semibold px-6 py-2.5 text-sm transition-colors"
            >
              {registrando ? 'Registrando...' : 'Registrar bloqueo'}
            </button>
          </form>
        </Card>
      </div>

      {mensaje && <Aviso mensaje={mensaje} onCerrar={() => setMensaje(null)} />}

      <div>
        <h2 className="flex items-center gap-2 text-lg font-bold text-(--color-ink-900) mb-3">
          <Construction className="w-5 h-5 text-(--color-brand-500)" />
          Bloqueos registrados
        </h2>
        <div className="bg-white rounded-xl border border-(--color-line-200) shadow-sm overflow-x-auto">
          <table className="w-full text-sm text-left">
            <thead className="bg-(--color-brand-500) text-white">
              <tr>
                {['Código', 'Inicio', 'Fin', 'Tramos', 'Origen', 'Estado'].map((c) => (
                  <th key={c} className="px-4 py-3 font-semibold whitespace-nowrap">{c}</th>
                ))}
              </tr>
            </thead>
            <tbody className="text-(--color-ink-700)">
              {cargando && (
                <tr><td colSpan={6} className="px-4 py-6 text-center text-(--color-ink-400)">Cargando bloqueos...</td></tr>
              )}
              {!cargando && bloqueos.length === 0 && (
                <tr><td colSpan={6} className="px-4 py-6 text-center text-(--color-ink-400)">Aún no hay bloqueos registrados.</td></tr>
              )}
              {bloqueos.map((b) => {
                const estado = derivarEstadoBloqueo(b.fechaHoraInicio, b.fechaHoraFin);
                return (
                  <tr key={b.id} className="border-b border-gray-100 odd:bg-white even:bg-(--color-brand-50)/50">
                    <td className="px-4 py-3 whitespace-nowrap font-semibold">{b.codigo}</td>
                    <td className="px-4 py-3 whitespace-nowrap">{formatFechaHora(b.fechaHoraInicio)}</td>
                    <td className="px-4 py-3 whitespace-nowrap">{formatFechaHora(b.fechaHoraFin)}</td>
                    <td className="px-4 py-3 whitespace-nowrap font-mono text-xs">{b.coordenadasNodos}</td>
                    <td className="px-4 py-3 whitespace-nowrap">{b.archivoOrigen === 'MANUAL' ? 'Manual' : 'Archivo'}</td>
                    <td className="px-4 py-3 whitespace-nowrap"><Badge tono={ESTADO_TONO[estado]}>{estado}</Badge></td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
};
