import React, { useState } from 'react';
import { Calendar, Info, Truck } from 'lucide-react';
import { ESCENARIOS } from '../../types/escenarios';
import type { Escenario } from '../../types/escenarios';
import { inputBase } from '../registro/estilos';
import { Campo } from '../registro/ui';

interface Props {
  escenario: Escenario;
  onEscenario: (e: Escenario) => void;
  fechaInicio: string;
  onFechaInicio: (v: string) => void;
  horaInicio: string;
  onHoraInicio: (v: string) => void;
  /** true = el motor ya está en ejecución (solo hay uno). */
  motorCorriendo: boolean;
  /** Nombre del escenario que ocupa el motor, si se sabe. */
  ocupadoPor: string | null;
  onIniciar: () => Promise<void>;
  onDetener: () => Promise<void>;
}

// TODO: leer estos rangos del backend (ConfiguracionSemaforo) cuando se conecte
const RANGOS = [
  { titulo: 'Crítico (replanificar inmediatamente)', valor: '≤ 0 h', clase: 'border-red-400 text-red-500' },
  { titulo: 'En riesgo (preparar planificación)', valor: '> 0 y < 4 h', clase: 'border-yellow-400 text-yellow-600' },
  { titulo: 'Óptimo (Continúa con la ruta)', valor: '≥ 4 h', clase: 'border-green-400 text-green-600' },
];

const IconoCalendario: React.FC<{ marca: string }> = ({ marca }) => (
  <span className="relative inline-flex justify-center text-[#5b83a3]">
    <Calendar className="w-16 h-16" strokeWidth={1.75} />
    <span className="absolute inset-x-0 top-6 text-center text-xl font-black leading-none">{marca}</span>
  </span>
);

const iconoDe = (id: Escenario) =>
  id === 'DIA_A_DIA' ? (
    <Truck className="w-16 h-16 text-[#5b83a3]" strokeWidth={1.75} />
  ) : id === 'SIMULACION_5D' ? (
    <IconoCalendario marca="5" />
  ) : (
    <IconoCalendario marca="!" />
  );

export const ConfiguracionPage: React.FC<Props> = ({
  escenario,
  onEscenario,
  fechaInicio,
  onFechaInicio,
  horaInicio,
  onHoraInicio,
  motorCorriendo,
  ocupadoPor,
  onIniciar,
  onDetener,
}) => {
  const [errores, setErrores] = useState<{ fecha?: string; hora?: string }>({});
  const [iniciando, setIniciando] = useState(false);

  const handleIniciar = async () => {
    const nuevos: { fecha?: string; hora?: string } = {};
    if (!fechaInicio) nuevos.fecha = 'Elige la fecha de inicio';
    if (!horaInicio) nuevos.hora = 'Elige la hora de inicio';
    setErrores(nuevos);
    if (Object.keys(nuevos).length > 0) return;

    setIniciando(true);
    try {
      await onIniciar();
    } finally {
      setIniciando(false);
    }
  };

  return (
    <main className="flex-1 p-6 md:p-8 space-y-6">
      {/* Configura el inicio */}
      <section className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
        <h2 className="text-lg font-bold text-gray-800 mb-4">Configura el inicio</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 items-start">
          <Campo etiqueta="Fecha de inicio *" error={errores.fecha}>
            <input type="date" value={fechaInicio} onChange={(e) => onFechaInicio(e.target.value)} className={inputBase} />
          </Campo>
          <Campo etiqueta="Hora de inicio *" error={errores.hora}>
            <input type="time" value={horaInicio} onChange={(e) => onHoraInicio(e.target.value)} className={inputBase} />
          </Campo>
          <div className="flex items-center gap-3 rounded-lg bg-blue-50 px-5 py-4 text-sm font-semibold text-gray-600">
            <Info className="w-5 h-5 text-blue-600 shrink-0" />
            <p>La simulación inicia desde la fecha y hora configuradas, se aplica a los tres escenarios.</p>
          </div>
        </div>
      </section>

      {/* Selecciona el escenario */}
      <section className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
        <h2 className="text-lg font-bold text-gray-800 mb-4">Selecciona el escenario</h2>
        <div role="radiogroup" className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {ESCENARIOS.map((e) => {
            const activo = e.id === escenario;
            return (
              <button
                key={e.id}
                type="button"
                role="radio"
                aria-checked={activo}
                onClick={() => onEscenario(e.id)}
                className={`rounded-xl border px-5 py-4 text-center transition-colors ${
                  activo ? 'border-blue-500 bg-blue-50 shadow-sm' : 'border-indigo-200 bg-white hover:bg-gray-50'
                }`}
              >
                <span className="flex items-center gap-3">
                  <span
                    className={`w-5 h-5 rounded-full border-2 flex items-center justify-center ${
                      activo ? 'border-blue-600' : 'border-gray-300'
                    }`}
                  >
                    {activo && <span className="w-2.5 h-2.5 rounded-full bg-blue-600" />}
                  </span>
                  <span className={`font-bold ${activo ? 'text-blue-600' : 'text-gray-900'}`}>{e.titulo}</span>
                </span>
                <span className="flex justify-center my-3">{iconoDe(e.id)}</span>
                <span className="block text-sm font-semibold text-gray-500">{e.descripcion}</span>
              </button>
            );
          })}
        </div>
      </section>

      {/* Aviso: el motor ya está corriendo */}
      {motorCorriendo && (
        <div className="flex flex-wrap items-center justify-between gap-3 text-sm text-amber-700 bg-amber-50 border border-amber-200 rounded-lg px-4 py-3">
          <span>
            {ocupadoPor
              ? `Hay una simulación en ejecución (${ocupadoPor}).`
              : 'Hay una simulación en ejecución.'}{' '}
            Solo puede haber una a la vez: deténla para iniciar otra.
          </span>
          <button
            type="button"
            onClick={() => onDetener()}
            className="rounded-lg bg-white border border-amber-300 px-3 py-1.5 font-semibold hover:bg-amber-100"
          >
            Detener simulación en ejecución
          </button>
        </div>
      )}

      <div className="flex flex-wrap items-end justify-between gap-6">
        <section className="bg-white rounded-xl border border-gray-200 shadow-sm p-6 w-full max-w-xl">
          <h2 className="text-lg font-bold text-gray-800 mb-3">Rangos del semáforo de criticidad</h2>
          <div className="space-y-2.5">
            {RANGOS.map((r) => (
              <div key={r.titulo} className={`flex items-center justify-between rounded-lg border px-3 py-1.5 text-sm font-bold ${r.clase}`}>
                <span>{r.titulo}</span>
                <span className="rounded-md border border-gray-200 bg-white px-3 py-0.5 text-gray-600 font-semibold">{r.valor}</span>
              </div>
            ))}
          </div>
        </section>

        <button
          type="button"
          onClick={handleIniciar}
          disabled={motorCorriendo || iniciando}
          className="rounded-xl bg-blue-600 hover:bg-blue-700 active:bg-blue-800 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold px-10 py-3 text-lg transition-colors"
        >
          Iniciar Simulación
        </button>
      </div>
    </main>
  );
};
