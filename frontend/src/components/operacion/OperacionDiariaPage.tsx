import React from 'react';
import { Pause, Play, Square, Timer } from 'lucide-react';
import { MapaOperaciones } from '../mapa/MapaOperaciones';
import { useIncidenciasMapa } from '../../hooks/useIncidenciasMapa';
import type { Almacen, Pedido, RelojSimuladoData, UnidadTransporte } from '../../types';
import { PanelOperacion } from './PanelOperacion';

interface Props {
  reloj: RelojSimuladoData;
  almacenes: Almacen[];
  unidades: UnidadTransporte[];
  pedidos: Pedido[];
  selectedUnidadId: number | null;
  onSelectUnidad: (id: number) => void;
  /** true = el motor corre con otro escenario (o uno desconocido): no se muestran datos en vivo. */
  sinDatos: boolean;
  /** Nombre del escenario que ocupa el motor, si se sabe. */
  ocupadoPor: string | null;
  /** Debe arrancar el motor con el escenario DIA_A_DIA. */
  onIniciar: () => Promise<void> | void;
  onDetener: () => Promise<void> | void;
  onFinalizar: () => Promise<void> | void;
}

const formatFecha = (iso: string) =>
  iso
    ? new Date(iso).toLocaleString('es-PE', {
        day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
      })
    : '--';

const horasTranscurridas = (reloj: RelojSimuladoData) => {
  if (!reloj.instanteActual) return '0 h';
  const horas = (reloj.diaSimulado - 1) * 24 + parseInt(reloj.horaSimulada.split(':')[0] || '0');
  return `${horas} h`;
};

const ESTADOS: Record<string, { texto: string; clase: string }> = {
  EN_EJECUCION: { texto: 'En ejecución', clase: 'bg-green-50 text-green-700 ring-green-200' },
  DETENIDA: { texto: 'Detenida', clase: 'bg-gray-100 text-gray-600 ring-gray-200' },
  FINALIZADA: { texto: 'Finalizada', clase: 'bg-gray-100 text-gray-600 ring-gray-200' },
  CONFIGURADA: { texto: 'Lista para iniciar', clase: 'bg-blue-50 text-blue-700 ring-blue-200' },
  COLAPSO_LOGISTICO: { texto: 'Colapso logístico', clase: 'bg-red-50 text-red-700 ring-red-200' },
};

const botonControl =
  'p-2.5 rounded-lg border border-blue-200 bg-white text-gray-900 hover:bg-blue-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors';

export const OperacionDiariaPage: React.FC<Props> = ({
  reloj,
  almacenes,
  unidades,
  pedidos,
  selectedUnidadId,
  onSelectUnidad,
  sinDatos,
  ocupadoPor,
  onIniciar,
  onDetener,
  onFinalizar,
}) => {
  const { bloqueos, averias } = useIncidenciasMapa(3000);
  const motorCorriendo = reloj.estadoEjecucion === 'EN_EJECUCION';
  // Hay un solo motor. Si lo usa otro escenario, esta pantalla no muestra sus datos.
  const estado = sinDatos
    ? { texto: motorCorriendo ? (ocupadoPor ? `${ocupadoPor} en ejecución` : 'Otro escenario en ejecución') : 'Sin datos de esta simulación', clase: 'bg-amber-50 text-amber-700 ring-amber-200' }
    : (ESTADOS[reloj.estadoEjecucion] ?? ESTADOS.DETENIDA!);

  const handleIniciar = async () => {
    try {
      await onIniciar();
    } catch {
      // El motor puede tardar en responder; el reloj se sigue consultando solo.
    }
  };

  return (
    <main className="flex-1 p-6 md:p-8 space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-8 text-sm text-gray-500 px-1">
          <div>
            Fecha: <span className="font-bold text-gray-900 ml-1">{formatFecha(sinDatos ? '' : reloj.instanteActual)}</span>
          </div>
          <div className="flex items-center">
            <Timer size={16} className="mr-1.5" />
            Tiempo transcurrido: <span className="font-bold text-gray-900 ml-1">{sinDatos ? '--' : horasTranscurridas(reloj)}</span>
          </div>
          <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ${estado.clase}`}>{estado.texto}</span>
        </div>

        <div className="flex items-center gap-2">
          <button type="button" onClick={handleIniciar} disabled={motorCorriendo} className={botonControl} title="Iniciar">
            <Play size={18} fill="currentColor" />
          </button>
          <button type="button" onClick={() => onDetener()} disabled={!motorCorriendo || sinDatos} className={botonControl} title="Detener">
            <Pause size={18} fill="currentColor" />
          </button>
          <button type="button" onClick={() => onFinalizar()} disabled={!motorCorriendo} className={botonControl} title="Finalizar">
            <Square size={18} fill="currentColor" />
          </button>
          <span
            className="ml-1 rounded-lg border border-blue-200 bg-white px-3 py-2 text-sm font-bold text-gray-900"
            title="Operación en tiempo real: 1 s real = 1 s simulado"
          >
            1.0×
          </span>
        </div>
      </div>

      {sinDatos && (
        <div className="flex flex-wrap items-center justify-between gap-3 text-sm text-amber-700 bg-amber-50 border border-amber-200 rounded-lg px-4 py-2">
          <span>
            {motorCorriendo
              ? ocupadoPor
                ? `El motor está ocupado con ${ocupadoPor}, por eso aquí no se muestran datos.`
                : 'Hay otra simulación en ejecución que no se inició desde esta sesión, por eso aquí no se muestran datos.'
              : `Los datos actuales son de ${ocupadoPor ?? 'otra simulación'}, por eso aquí no se muestran. Presiona ▶ para iniciar la operación día a día.`}
          </span>
          {motorCorriendo && (
            <button
              type="button"
              onClick={() => onFinalizar()}
              className="rounded-lg bg-white border border-amber-300 px-3 py-1 font-semibold hover:bg-amber-100"
            >
              Detener simulación en ejecución
            </button>
          )}
        </div>
      )}

      <div className="flex items-start gap-6">
        <div className="flex-1 min-w-0">
          <MapaOperaciones
            almacenes={almacenes}
            unidades={sinDatos ? [] : unidades}
            pedidos={sinDatos ? [] : pedidos}
            selectedUnidadId={selectedUnidadId}
            onSelectUnidad={onSelectUnidad}
            bloqueos={sinDatos ? [] : bloqueos}
          />
        </div>
        <PanelOperacion
          reloj={reloj}
          almacenes={almacenes}
          unidades={unidades}
          pedidos={pedidos}
          totalBloqueos={bloqueos.length}
          totalAverias={averias.length}
          sinDatos={sinDatos}
        />
      </div>
    </main>
  );
};
