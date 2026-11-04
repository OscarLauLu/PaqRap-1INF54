import React, { useEffect, useState } from 'react';
import { AlertTriangle, FileText, Map as MapIcon } from 'lucide-react';
import type { MetricasSimulacion } from '../../types';

interface Props {
  metricas: MetricasSimulacion;
  /** Solo se muestra en la pantalla de simulación y si los datos son de este escenario */
  visible: boolean;
  /** Botón "Ver reporte": lleva a la página de Reportes */
  onVerReporte: () => void;
}

/** Lee el primer campo con valor entre varios nombres posibles (el backend puede llamarlos distinto) */
const campo = (obj: object, nombres: string[]): unknown => {
  const o = obj as Record<string, unknown>;
  for (const n of nombres) if (o[n] !== undefined && o[n] !== null && o[n] !== '') return o[n];
  return undefined;
};

/**
 * Alerta grande de colapso logístico, al centro de la pantalla.
 * Aparece una sola vez por colapso (si se cierra, no vuelve a salir para el mismo instante).
 */
export const AlertaColapso: React.FC<Props> = ({ metricas, visible, onVerReporte }) => {
  const instante = metricas.instanteColapso ?? null;
  const [cerradoPara, setCerradoPara] = useState<string | null>(null);
  const abierta = visible && !!instante && cerradoPara !== instante;

  // Esc cierra la alerta
  useEffect(() => {
    if (!abierta) return;
    const alTeclear = (e: KeyboardEvent) => { if (e.key === 'Escape') setCerradoPara(instante); };
    window.addEventListener('keydown', alTeclear);
    return () => window.removeEventListener('keydown', alTeclear);
  }, [abierta, instante]);

  if (!abierta || !instante) return null;

  const pedido = campo(metricas, ['codigoPedidoColapso', 'pedidoColapso', 'pedidoIncumplido']);
  const fecha = new Date(instante).toLocaleString('es-PE', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
  });

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-gray-900/40 backdrop-blur-[2px] p-4" role="alertdialog" aria-modal="true" aria-labelledby="titulo-colapso">
      <div className="w-full max-w-md rounded-2xl bg-white shadow-2xl overflow-hidden animate-in fade-in zoom-in duration-200">
        {/* Franja roja con el ícono */}
        <div className="bg-red-600 px-6 py-5 text-center text-white">
          <div className="mx-auto mb-2 flex h-14 w-14 items-center justify-center rounded-full bg-white/15 ring-4 ring-white/20 animate-pulse">
            <AlertTriangle className="h-8 w-8" />
          </div>
          <h2 id="titulo-colapso" className="text-xl font-extrabold tracking-wide">¡Colapso logístico!</h2>
          <p className="mt-1 text-sm text-white/90">Un pedido no pudo entregarse dentro de su plazo.</p>
        </div>

        {/* Datos del colapso */}
        <div className="px-6 py-5 space-y-3 text-sm">
          <div className="flex justify-between gap-4">
            <span className="text-(--color-ink-500)">Momento del colapso</span>
            <span className="font-bold text-(--color-ink-900) text-right">{fecha}</span>
          </div>
          {typeof metricas.volumenPedidosColapso === 'number' && (
            <div className="flex justify-between gap-4">
              <span className="text-(--color-ink-500)">Pedidos activos en ese instante</span>
              <span className="font-bold text-(--color-ink-900)">{metricas.volumenPedidosColapso}</span>
            </div>
          )}
          {pedido !== undefined && (
            <div className="flex justify-between gap-4">
              <span className="text-(--color-ink-500)">Pedido incumplido</span>
              <span className="font-bold text-red-600">{String(pedido)}</span>
            </div>
          )}
          <div className="flex justify-between gap-4">
            <span className="text-(--color-ink-500)">Incidencias hasta ese momento</span>
            <span className="font-bold text-(--color-ink-900)">
              {metricas.bloqueosOcurridos} bloqueos · {metricas.averiasOcurridas} averías
            </span>
          </div>
          <p className="rounded-lg bg-red-50 border border-red-100 px-3 py-2 text-xs text-red-700">
            Esta es la carga máxima que el planificador logró sostener. La simulación se detuvo.
          </p>
        </div>

        {/* Acciones */}
        <div className="flex gap-3 px-6 pb-6">
          <button
            type="button"
            onClick={() => setCerradoPara(instante)}
            className="flex-1 flex items-center justify-center gap-2 rounded-xl border border-(--color-line-200) px-4 py-2.5 text-sm font-semibold text-(--color-ink-700) hover:bg-gray-50"
          >
            <MapIcon size={16} /> Ver el mapa
          </button>
          <button
            type="button"
            onClick={() => { setCerradoPara(instante); onVerReporte(); }}
            className="flex-1 flex items-center justify-center gap-2 rounded-xl bg-red-600 hover:bg-red-700 px-4 py-2.5 text-sm font-semibold text-white"
            autoFocus
          >
            <FileText size={16} /> Ver reporte
          </button>
        </div>
      </div>
    </div>
  );
};