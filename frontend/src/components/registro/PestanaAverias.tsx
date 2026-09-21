import React, { useRef, useState } from 'react';
import { FileText, Upload } from 'lucide-react';
import { averiasApi } from '../../api/averiasApi';
import type { EstadoAveria, TipoAveria } from '../../types/registro';
import { inputBase } from './estilos';
import { Aviso, Campo, Selector } from './ui';
import {
  ahoraHHmm,
  combinarFechaHora,
  formatFechaHora,
  hoyISO,
  parseUbicacion,
  sumarHoras,
} from './utils';

const TIPOS = [
  { valor: 'TIPO_1', etiqueta: 'Tipo 1' },
  { valor: 'TIPO_2', etiqueta: 'Tipo 2' },
  { valor: 'TIPO_3', etiqueta: 'Tipo 3' },
];

const ESTADOS = [
  { valor: 'En reparación', etiqueta: 'En reparación' },
  { valor: 'Resuelta', etiqueta: 'Resuelta' },
];

// TODO: confirmar con el grupo cuánto dura la indisponibilidad según el tipo de avería
const HORAS_SUGERIDAS = 2;

interface Errores {
  idUnidad?: string;
  ubicacion?: string;
  fecha?: string;
  hora?: string;
  fin?: string;
}

export const PestanaAverias: React.FC = () => {
  const [idUnidad, setIdUnidad] = useState('');
  const [ubicacion, setUbicacion] = useState('');
  const [fecha, setFecha] = useState(() => hoyISO());
  const [hora, setHora] = useState(() => ahoraHHmm());
  const [tipo, setTipo] = useState<TipoAveria>('TIPO_1');
  const [fin, setFin] = useState(() => sumarHoras(hora, HORAS_SUGERIDAS));
  const [finEditado, setFinEditado] = useState(false);
  const [estado, setEstado] = useState<EstadoAveria>('En reparación');
  const [errores, setErrores] = useState<Errores>({});
  const [mensaje, setMensaje] = useState<string | null>(null);
  const inputArchivo = useRef<HTMLInputElement>(null);

  // Mientras el usuario no edite el fin, se sugiere hora de avería + 2 h
  const cambiarHora = (nueva: string) => {
    setHora(nueva);
    if (!finEditado && nueva) setFin(sumarHoras(nueva, HORAS_SUGERIDAS));
  };

  const cambiarFin = (nuevo: string) => {
    setFin(nuevo);
    setFinEditado(true);
  };

  const limpiar = () => {
    const h = ahoraHHmm();
    setIdUnidad('');
    setUbicacion('');
    setFecha(hoyISO());
    setHora(h);
    setTipo('TIPO_1');
    setFin(sumarHoras(h, HORAS_SUGERIDAS));
    setFinEditado(false);
    setEstado('En reparación');
    setErrores({});
  };

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    const ubic = parseUbicacion(ubicacion);
    const nuevos: Errores = {};
    if (!idUnidad.trim()) nuevos.idUnidad = 'Ingresa el ID de la unidad';
    if (!ubic) nuevos.ubicacion = 'Usa el formato (x,y). Ej: (32,18)';
    if (!fecha) nuevos.fecha = 'Elige la fecha';
    if (!hora) nuevos.hora = 'Elige la hora';
    if (!fin) nuevos.fin = 'Elige la hora de fin';

    setErrores(nuevos);
    if (!ubic || Object.keys(nuevos).length > 0) return;

    const inicio = combinarFechaHora(fecha, hora);
    let finFecha = combinarFechaHora(fecha, fin);
    // Si el fin es menor o igual a la hora de avería, termina al día siguiente
    if (finFecha <= inicio) finFecha = new Date(finFecha.getTime() + 24 * 3600000);

    const registrada = await averiasApi.registrar({
      idUnidad: idUnidad.trim().toUpperCase(),
      ubicacion: ubic,
      tipo,
      inicio: inicio.toISOString(),
      fin: finFecha.toISOString(),
      estado,
    });

    setMensaje(
      `Avería registrada para ${registrada.idUnidad}. No disponible hasta ${formatFechaHora(registrada.fin)}. Modo demostración: aún no se guarda en el servidor.`,
    );
    limpiar();
  };

  const handleArchivo = (e: React.ChangeEvent<HTMLInputElement>) => {
    const archivo = e.target.files?.[0];
    if (archivo) {
      // TODO: leer el archivo y registrar las averías
      setMensaje(`Archivo "${archivo.name}" seleccionado. La importación se conecta más adelante.`);
    }
    e.target.value = '';
  };

  return (
    <>
      <form
        id="form-nueva-averia"
        onSubmit={handleSubmit}
        noValidate
        className="bg-white rounded-xl border border-gray-200 shadow-sm p-6"
      >
        <h2 className="flex items-center gap-2 text-lg font-bold text-gray-800 mb-4">
          <FileText className="w-5 h-5 text-blue-600" />
          Nueva avería
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-x-6 gap-y-4">
          <Campo etiqueta="ID Unidad" error={errores.idUnidad}>
            <input
              type="text"
              value={idUnidad}
              onChange={(e) => setIdUnidad(e.target.value)}
              placeholder="AUTO-03"
              className={inputBase}
            />
          </Campo>

          <Campo etiqueta="Ubicación" error={errores.ubicacion}>
            <input
              type="text"
              value={ubicacion}
              onChange={(e) => setUbicacion(e.target.value)}
              placeholder="(32,18)"
              className={inputBase}
            />
          </Campo>

          <Campo etiqueta="Fecha de avería" error={errores.fecha}>
            <input type="date" value={fecha} onChange={(e) => setFecha(e.target.value)} className={inputBase} />
          </Campo>

          <Campo etiqueta="Hora de avería" error={errores.hora}>
            <input type="time" value={hora} onChange={(e) => cambiarHora(e.target.value)} className={inputBase} />
          </Campo>

          <Campo etiqueta="Tipo de avería">
            <Selector value={tipo} onChange={(v) => setTipo(v as TipoAveria)} opciones={TIPOS} />
          </Campo>

          <Campo etiqueta="Fin de indisponibilidad" error={errores.fin}>
            <input type="time" value={fin} onChange={(e) => cambiarFin(e.target.value)} className={inputBase} />
          </Campo>

          <Campo etiqueta="Estado">
            <Selector value={estado} onChange={(v) => setEstado(v as EstadoAveria)} opciones={ESTADOS} />
          </Campo>
        </div>
      </form>

      <div className="flex justify-end items-center gap-3">
        <input ref={inputArchivo} type="file" accept=".txt,.csv" onChange={handleArchivo} className="hidden" />
        <button
          type="button"
          onClick={() => inputArchivo.current?.click()}
          className="flex items-center gap-2 rounded-xl bg-gray-500 hover:bg-gray-600 text-white font-semibold px-5 py-2.5 text-sm transition-colors"
        >
          <Upload className="w-4 h-4" />
          Cargar archivo
        </button>
        <button
          type="submit"
          form="form-nueva-averia"
          className="rounded-xl bg-blue-600 hover:bg-blue-700 active:bg-blue-800 text-white font-semibold px-6 py-2.5 text-sm transition-colors"
        >
          Registrar avería
        </button>
      </div>

      {mensaje && <Aviso mensaje={mensaje} onCerrar={() => setMensaje(null)} />}
    </>
  );
};
