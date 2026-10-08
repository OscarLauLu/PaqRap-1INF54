import React, { useEffect, useRef, useState } from 'react';
import { FileUp, FileText, Info, Construction, Upload } from 'lucide-react';
import { bloqueosApi } from '../../api/bloqueosApi';
import type { BloqueoVial } from '../../types';
import { derivarEstadoBloqueo } from '../../types/registro';
import { Card } from '../ui/Card';
import { Badge } from '../ui/Badge';
import { Aviso } from './ui';
import { formatFechaHora } from './utils';

const ESTADO_TONO = { Programado: 'azul', Activo: 'rojo', Vencido: 'gris' } as const;

export const PestanaBloqueos: React.FC = () => {
  const [bloqueos, setBloqueos] = useState<BloqueoVial[]>([]);
  const [cargando, setCargando] = useState(true);

  // --- Flujo 1: subir archivo mensual de bloqueos ---
  const [archivo, setArchivo] = useState<File | null>(null);
  const [subiendo, setSubiendo] = useState(false);
  const inputArchivo = useRef<HTMLInputElement>(null);

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

  return (
    <>
      {/* ===== Nuevo bloqueo (diseño del mockup) ===== */}
      <div className="space-y-4">
        <Card icono={FileText} titulo="Nuevo bloqueo">
          <input ref={inputArchivo} type="file" accept=".txt,.csv" onChange={handleArchivo} className="hidden" />
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 items-center">
            {/* Zona para seleccionar el archivo */}
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

            {/* Recuadro informativo */}
            <div className="flex items-center gap-3 rounded-md bg-(--color-brand-50) px-6 py-10 text-sm font-semibold text-(--color-ink-500)">
              <Info className="w-6 h-6 text-(--color-brand-500) shrink-0" />
              <p>
                El archivo a subir debe contener los bloqueos de las calles en el formato adecuado
                <span className="block mt-1 text-xs font-normal text-(--color-ink-400)">
                </span>
              </p>
            </div>
          </div>
        </Card>

        {/* Botones de acción alineados a la derecha */}
        <div className="flex flex-wrap justify-end gap-4">
          <button
            type="button"
            onClick={abrirSelector}
            className="flex items-center gap-2 rounded-lg bg-gray-500 hover:bg-gray-600 text-white font-semibold px-5 py-2.5 text-sm transition-colors"
          >
            <Upload className="w-4 h-4" />
            Cargar archivo
          </button>
          <button
            type="button"
            onClick={subirArchivo}
            disabled={!archivo || subiendo}
            className="rounded-lg bg-(--color-brand-500) hover:bg-(--color-brand-600) disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold px-7 py-2.5 text-sm transition-colors"
          >
            {subiendo ? 'Procesando...' : 'Registrar bloqueo'}
          </button>
        </div>
      </div>

      {mensaje && <Aviso mensaje={mensaje} onCerrar={() => setMensaje(null)} />}

      {/* ===== Tabla (sin cambios) ===== */}
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