import React, { useEffect, useState } from 'react';
import { Truck, CalendarDays, AlertTriangle, Info, Rocket, Loader2, CalendarClock, Layers } from 'lucide-react';
import { Card } from '../ui/Card';
import { simulacionApi } from '../../api/simulacionApi';
import { setBackend } from '../../api/client';
import type { TipoEscenario } from '../../types';

interface Props {
  onSimulacionIniciada: (escenario: TipoEscenario) => void;
}

const ESCENARIOS: { id: TipoEscenario; titulo: string; detalle: string; Icono: React.ElementType }[] = [
  { id: 'DIA_A_DIA', titulo: 'Operación día a día', detalle: 'Operación en tiempo real de las entregas, sin duración fija.', Icono: Truck },
  { id: 'SIMULACION_5D', titulo: 'Simulación 5D', detalle: '5 días simulados de operación logística (30–60 min de ejecución real).', Icono: CalendarDays },
  { id: 'COLAPSO_LOGISTICO', titulo: 'Simulación de colapso', detalle: 'Corre hasta que un solo pedido incumpla su plazo límite.', Icono: AlertTriangle },
];

// Flota por defecto y capacidad reglamentaria de almacenes intermedios
const FLOTA_POR_DEFECTO = {
  numAutos: 10,
  numMotos: 10,
  numBicicletas: 10,
  capacidadAlmacenIntermedio: 1000,
  tasaIncrementoPedidos: 1.0,
};

const inputBase =
  'w-full rounded-xl border border-ink-300 bg-white px-4 py-2.5 text-base text-ink-900 outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100 transition-colors';

const inputUmbral =
  'w-12 rounded-md border border-ink-300 bg-white px-1 py-0.5 text-center text-sm font-bold text-ink-900 outline-none focus:border-brand-500';

export const ConfiguracionPage: React.FC<Props> = ({ onSimulacionIniciada }) => {
  const [escenario, setEscenario] = useState<TipoEscenario>('DIA_A_DIA');

  // Sin valores por defecto: el usuario debe elegir la fecha y la hora de inicio de forma explícita
  const [fechaInicio, setFechaInicio] = useState('');
  const [horaInicio, setHoraInicio] = useState('');

  const [horasVerde, setHorasVerde] = useState(12);
  const [horasAmbar, setHorasAmbar] = useState(4);

  const [iniciando, setIniciando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    simulacionApi.obtenerSemaforo().then((s) => {
      if (s) {
        if (s.horasVerde !== undefined && s.horasVerde !== null) setHorasVerde(s.horasVerde);
        if (s.horasAmbar !== undefined && s.horasAmbar !== null) setHorasAmbar(s.horasAmbar);
      }
    });
  }, []);

  const iniciar = async () => {
    setIniciando(true);
    setError(null);

    if (!fechaInicio || !horaInicio) {
      setError('Ingresa la fecha y la hora de inicio.');
      setIniciando(false);
      return;
    }
    if (horasAmbar >= horasVerde) {
      setError('El umbral Ámbar debe ser menor que el umbral Verde.');
      setIniciando(false);
      return;
    }

    if (escenario === 'DIA_A_DIA') {
      try {
        setBackend('operacion');
        try {
          await simulacionApi.actualizarSemaforo(horasVerde, horasAmbar);
        } catch (e) {
          console.warn('Aviso al actualizar semáforo:', e);
        }
        try {
          await simulacionApi.configurar({
            ...FLOTA_POR_DEFECTO,
            fechaHoraInicio: `${fechaInicio}T${horaInicio}:00`,
          });
        } catch (e) {
          console.warn('Aviso al configurar operación:', e);
        }
        try {
          await simulacionApi.iniciarSimulacion('DIA_A_DIA');
        } catch (e) {
          console.warn('La operación día a día ya estaba en ejecución:', e);
        }
        onSimulacionIniciada(escenario);
      } catch (err: any) {
        console.error('Error al iniciar operación día a día:', err);
        const url = err?.config?.url || '';
        const method = err?.config?.method?.toUpperCase() || 'POST';
        const status = err?.response?.status || err?.status || '';
        const msg = err?.response?.data?.message || err?.response?.data || err?.message;
        setError(`No se pudo iniciar la operación día a día (${method} ${url} ${status}): ${msg}`);
      } finally {
        setIniciando(false);
      }
      return;
    }

    // Escenarios de simulación (5D o Colapso)
    try {
      setBackend('simulacion');
      try {
        await simulacionApi.actualizarSemaforo(horasVerde, horasAmbar);
      } catch (e) {
        console.warn('Aviso al actualizar semáforo:', e);
      }
      try {
        await simulacionApi.configurar({
          ...FLOTA_POR_DEFECTO,
          archivoPedidos: undefined,
          archivoBloqueos: undefined,
          fechaHoraInicio: `${fechaInicio}T${horaInicio}:00`,
        });
      } catch (e) {
        console.warn('Aviso al configurar simulación (se usarán datos por defecto):', e);
      }
      await simulacionApi.iniciarSimulacion(escenario);
      onSimulacionIniciada(escenario);
    } catch (err: any) {
      console.error('Error al iniciar simulación:', err);
      const url = err?.config?.url || '';
      const method = err?.config?.method?.toUpperCase() || 'POST';
      const status = err?.response?.status || err?.status || '';
      const msg = err?.response?.data?.message || err?.response?.data || err?.message;
      setError(`No se pudo iniciar la simulación (${method} ${url} ${status}): ${msg}`);
    } finally {
      setIniciando(false);
    }
  };

  return (
    <main className="flex-1 p-6 md:p-8 space-y-6">
      {/* ===== Configura el inicio ===== */}
      <Card icono={CalendarClock} titulo="Configura el inicio">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 items-stretch -mt-3">
          <label className="block">
            <span className="block text-sm font-semibold text-ink-700 mb-1.5">
              Fecha de inicio <span className="text-red-500">*</span>
            </span>
            <input type="date" value={fechaInicio} onChange={(e) => setFechaInicio(e.target.value)} className={inputBase} />
          </label>

          <label className="block">
            <span className="block text-sm font-semibold text-ink-700 mb-1.5">
              Hora de inicio <span className="text-red-500">*</span>
            </span>
            <input type="time" value={horaInicio} onChange={(e) => setHoraInicio(e.target.value)} className={inputBase} />
          </label>

          <div className="h-full flex items-center gap-3 rounded-md bg-brand-50 px-5 py-3 text-sm text-ink-500">
            <Info className="w-5 h-5 text-brand-500 shrink-0" />
            <p>La simulación inicia desde la fecha y hora configuradas, se aplica a los tres escenarios.</p>
          </div>
        </div>
      </Card>

      {/* ===== Selecciona el escenario ===== */}
      <Card icono={Layers} titulo="Selecciona el escenario">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4" role="radiogroup">
          {ESCENARIOS.map(({ id, titulo, detalle, Icono }) => {
            const activo = escenario === id;
            return (
              <button
                key={id}
                type="button"
                role="radio"
                aria-checked={activo}
                onClick={() => setEscenario(id)}
                className={`rounded-xl border-2 p-5 flex flex-col items-center gap-3 transition-colors ${
                  activo ? 'border-brand-500 bg-brand-50' : 'border-line-200 bg-white hover:bg-gray-50'
                }`}
              >
                <span className="flex items-center gap-3 self-start">
                  <span
                    className={`w-5 h-5 rounded-full border-2 flex items-center justify-center shrink-0 ${
                      activo ? 'border-brand-500' : 'border-ink-300'
                    }`}
                  >
                    {activo && <span className="w-2.5 h-2.5 rounded-full bg-brand-500" />}
                  </span>
                  <span className={`font-bold ${activo ? 'text-brand-500' : 'text-ink-900'}`}>{titulo}</span>
                </span>

                {/* Ícono: negro sin seleccionar, azul al seleccionar */}
                <Icono className={`w-12 h-12 transition-colors ${activo ? 'text-brand-500' : 'text-ink-900'}`} />

                <span className="text-xs text-ink-400 text-center">{detalle}</span>
              </button>
            );
          })}
        </div>
      </Card>

      {error && (
        <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-2.5 text-sm text-red-700">{error}</div>
      )}

      {/* ===== Semáforo (ancho completo) + botón arriba a su derecha ===== */}
      <div className="flex flex-col md:flex-row gap-6 md:items-start">
        <div className="flex-1 min-w-0">
          <Card icono={Info} titulo="Rangos del semáforo de criticidad">
            <p className="text-xs text-ink-400 mb-3 -mt-2">
              Horas de holgura restante hasta el plazo límite. Cambiarlos aplica en caliente, sin reiniciar el sistema.
            </p>
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-3">
              <div className="flex items-center justify-between gap-3 rounded-lg border border-red-200 bg-red-50 px-4 py-2.5">
                <span className="leading-tight">
                  <span className="block text-sm font-bold text-red-600">Crítico</span>
                  <span className="block text-xs text-ink-500">Replanificar inmediatamente</span>
                </span>
                <span className="flex items-center gap-1 text-sm font-bold text-ink-700 shrink-0">
                  &lt;
                  <input type="number" min={0} value={horasAmbar} onChange={(e) => setHorasAmbar(Number(e.target.value))}
                    className={inputUmbral} aria-label="Límite de crítico (horas)" />
                  h
                </span>
              </div>

              <div className="flex items-center justify-between gap-3 rounded-lg border border-amber-200 bg-amber-50 px-4 py-2.5">
                <span className="leading-tight">
                  <span className="block text-sm font-bold text-amber-700">En riesgo</span>
                  <span className="block text-xs text-ink-500">Preparar planificación</span>
                </span>
                <span className="text-sm font-bold text-ink-700 shrink-0">
                  {horasAmbar}–{horasVerde} h
                </span>
              </div>

              <div className="flex items-center justify-between gap-3 rounded-lg border border-green-200 bg-green-50 px-4 py-2.5">
                <span className="leading-tight">
                  <span className="block text-sm font-bold text-green-700">Óptimo</span>
                  <span className="block text-xs text-ink-500">Continúa con la ruta</span>
                </span>
                <span className="flex items-center gap-1 text-sm font-bold text-ink-700 shrink-0">
                  ≥
                  <input type="number" min={0} value={horasVerde} onChange={(e) => setHorasVerde(Number(e.target.value))}
                    className={inputUmbral} aria-label="Límite de óptimo (horas)" />
                  h
                </span>
              </div>
            </div>
          </Card>
        </div>

        <button
          type="button"
          onClick={iniciar}
          disabled={iniciando}
          className="self-end md:self-auto shrink-0 flex items-center justify-center gap-2 rounded-xl bg-brand-500 hover:bg-brand-600 active:bg-brand-700 disabled:opacity-50 text-white font-bold px-8 py-3 text-base transition-colors"
        >
          {iniciando ? <Loader2 className="w-4 h-4 animate-spin" /> : <Rocket className="w-4 h-4" />}
          Iniciar Simulación
        </button>
      </div>
    </main>
  );
};