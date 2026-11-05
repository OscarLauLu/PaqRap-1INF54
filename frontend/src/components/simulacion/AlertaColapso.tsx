import React, { useEffect, useState } from 'react';
import { AlertTriangle, FileText, Map as MapIcon } from 'lucide-react';
import type { MetricasSimulacion } from '../../types';

interface Props {
  metricas: MetricasSimulacion;
  /** true cuando el reloj ya informa que la simulación se detuvo por colapso (DETENIDA_POR_COLAPSO) */
  detenidaPorColapso: boolean;
  /** Solo se muestra en la pantalla del escenario de colapso */
  visible: boolean;
  /** Botón "Ver reporte": lleva a la página de Reportes */
  onVerReporte: () => void;
}

/**
 * Ventana de emergencia modal: el sistema colapsa en el instante en que UN pedido incumple su plazo
 * y la simulación se detiene. Ocupa toda la pantalla en rojo y se queda hasta que el usuario decide
 * (Ver reporte / Ver el mapa). Una vez cerrada no vuelve a salir para el mismo colapso.
 */
export const AlertaColapso: React.FC<Props> = ({ metricas, detenidaPorColapso, visible, onVerReporte }) => {
  // Hay colapso si las métricas traen el instante o si el reloj ya informa la detención por colapso
  const hayColapso = !!metricas.instanteColapso || detenidaPorColapso;
  const [cerrada, setCerrada] = useState(false);
  const abierta = visible && hayColapso && !cerrada;

  // Al iniciar otra corrida (ya no hay colapso) la ventana queda lista para el próximo
  const [habiaColapso, setHabiaColapso] = useState(hayColapso);
  if (hayColapso !== habiaColapso) {
    setHabiaColapso(hayColapso);
    if (!hayColapso) setCerrada(false);
  }

  // Esc cierra la ventana
  useEffect(() => {
    if (!abierta) return;
    const alTeclear = (e: KeyboardEvent) => { if (e.key === 'Escape') setCerrada(true); };
    window.addEventListener('keydown', alTeclear);
    return () => window.removeEventListener('keydown', alTeclear);
  }, [abierta]);

  if (!abierta) return null;

  const fecha = metricas.instanteColapso
    ? new Date(metricas.instanteColapso).toLocaleString('es-PE', {
        day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
      })
    : null;
  const cerrar = () => setCerrada(true);

  return (
    <div
      className="fixed inset-0 z-[9999] flex items-center justify-center bg-red-950/80 backdrop-blur-sm p-4"
      role="alertdialog"
      aria-modal="true"
      aria-labelledby="titulo-colapso"
      aria-describedby="detalle-colapso"
    >
      <div className="w-full max-w-lg overflow-hidden rounded-2xl border-4 border-red-600 bg-white shadow-[0_0_80px_rgba(220,38,38,0.7)]">
        {/* Cabecera de emergencia */}
        <div className="bg-red-600 px-6 py-6 text-center text-white">
          <div className="mx-auto mb-3 flex h-20 w-20 items-center justify-center rounded-full bg-white ring-8 ring-red-400/60 animate-pulse">
            <AlertTriangle className="h-12 w-12 text-red-600" strokeWidth={2.5} />
          </div>
          <p className="text-xs font-bold uppercase tracking-[0.3em] text-red-100">Emergencia</p>
          <h2 id="titulo-colapso" className="mt-1 text-3xl font-extrabold uppercase leading-tight">
            Se llegó al colapso logístico
          </h2>
          <p id="detalle-colapso" className="mt-2 text-sm text-red-50">
            Un pedido no pudo entregarse dentro de su plazo. La simulación se detuvo.
          </p>
        </div>

        {/* Datos del colapso */}
        <div className="space-y-3 px-6 py-5 text-sm">
          {fecha && (
            <div className="flex justify-between gap-4 border-b border-red-100 pb-2">
              <span className="text-gray-500">Momento del colapso</span>
              <span className="text-right font-bold text-gray-900">{fecha}</span>
            </div>
          )}
          {typeof metricas.volumenPedidosColapso === 'number' && (
            <div className="flex justify-between gap-4 border-b border-red-100 pb-2">
              <span className="text-gray-500">Pedidos activos en ese instante</span>
              <span className="font-bold text-red-700">{metricas.volumenPedidosColapso}</span>
            </div>
          )}
          {metricas.codigoPedidoColapso && (
            <div className="flex justify-between gap-4 border-b border-red-100 pb-2">
              <span className="text-gray-500">Pedido incumplido</span>
              <span className="font-bold text-red-700">{metricas.codigoPedidoColapso}</span>
            </div>
          )}
          <div className="flex justify-between gap-4">
            <span className="text-gray-500">Incidencias hasta ese momento</span>
            <span className="text-right font-bold text-gray-900">
              {metricas.bloqueosOcurridos} bloqueos · {metricas.averiasOcurridas} averías
            </span>
          </div>
          <p className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-800">
            Esta es la carga máxima que el planificador logró sostener antes de fallar.
          </p>
        </div>

        {/* Acciones */}
        <div className="flex gap-3 px-6 pb-6">
          <button
            type="button"
            onClick={cerrar}
            className="flex flex-1 items-center justify-center gap-2 rounded-xl border-2 border-red-200 px-4 py-3 text-sm font-semibold text-red-800 hover:bg-red-50"
          >
            <MapIcon size={16} /> Ver el mapa
          </button>
          <button
            type="button"
            onClick={() => { cerrar(); onVerReporte(); }}
            className="flex flex-1 items-center justify-center gap-2 rounded-xl bg-red-600 px-4 py-3 text-sm font-bold text-white hover:bg-red-700"
            autoFocus
          >
            <FileText size={16} /> Ver reporte
          </button>
        </div>
      </div>
    </div>
  );
};
