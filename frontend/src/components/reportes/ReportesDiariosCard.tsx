import React, { useEffect, useState } from 'react';
import { CalendarDays, ChevronDown, ChevronRight, Download } from 'lucide-react';
import { Card } from '../ui/Card';
import {
  EVENTO_REPORTES,
  descargarReporteDiarioCSV,
  formatMinutos,
  listarReportesDiarios,
  type ReporteDiario,
} from '../../utils/reportesDiarios';

const formatSoles = (valor: number) => `S/ ${valor.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
const formatDia = (fecha: string) => {
  const [a, m, d] = fecha.split('-');
  return a && m && d ? `${d}/${m}/${a}` : fecha;
};

/**
 * Reportes diarios de la Operación día a día (G09).
 * Se generan solos al cambiar de día y al detener la operación; aquí se listan y se descargan.
 */
export const ReportesDiariosCard: React.FC = () => {
  const [reportes, setReportes] = useState<ReporteDiario[]>(() => listarReportesDiarios());
  const [abierto, setAbierto] = useState<string | null>(null);

  // Se actualiza solo cuando se genera un reporte nuevo (en esta pestaña o en otra)
  useEffect(() => {
    const recargar = () => setReportes(listarReportesDiarios());
    window.addEventListener(EVENTO_REPORTES, recargar);
    window.addEventListener('storage', recargar);
    return () => {
      window.removeEventListener(EVENTO_REPORTES, recargar);
      window.removeEventListener('storage', recargar);
    };
  }, []);

  const lista = [...reportes].sort((a, b) => b.fecha.localeCompare(a.fecha)); // el más reciente primero

  return (
    <Card titulo="Reportes diarios de la operación día a día">
      {lista.length === 0 ? (
        <div className="flex items-center gap-3 py-2 text-sm text-ink-500">
          <CalendarDays className="w-5 h-5 text-brand-500 shrink-0" />
          <span>
            Todavía no hay reportes. Se generan automáticamente al cambiar de día en la operación día a día y al detenerla con ■.
          </span>
        </div>
      ) : (
        <div className="overflow-x-auto rounded-lg border border-gray-200">
          <table className="w-full text-xs text-left">
            <thead className="bg-gray-50 border-b border-gray-200 text-ink-500 uppercase font-semibold">
              <tr>
                <th className="px-3 py-2 w-6" />
                <th className="px-3 py-2">Día</th>
                <th className="px-3 py-2">Origen</th>
                <th className="px-3 py-2 text-right">Entregados</th>
                <th className="px-3 py-2 text-right">A tiempo</th>
                <th className="px-3 py-2 text-right">Tiempo prom. entrega</th>
                <th className="px-3 py-2 text-right">Costo</th>
                <th className="px-3 py-2 text-right">Averías</th>
                <th className="px-3 py-2 text-right">Bloqueos</th>
                <th className="px-3 py-2 text-right">CSV</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {lista.map((r) => {
                const expandido = abierto === r.id;
                return (
                  <React.Fragment key={r.id}>
                    <tr className="hover:bg-gray-50/80 transition-colors cursor-pointer" onClick={() => setAbierto(expandido ? null : r.id)}>
                      <td className="px-3 py-2 text-ink-400">{expandido ? <ChevronDown size={14} /> : <ChevronRight size={14} />}</td>
                      <td className="px-3 py-2 font-bold text-ink-900">{formatDia(r.fecha)}</td>
                      <td className="px-3 py-2">
                        <span
                          className={`px-2 py-0.5 rounded-full text-[10px] font-bold border ${
                            r.motivo === 'CIERRE' ? 'bg-amber-50 text-amber-700 border-amber-200' : 'bg-blue-50 text-blue-700 border-blue-200'
                          }`}
                        >
                          {r.motivo === 'CIERRE' ? 'Al detener' : 'Automático'}
                        </span>
                      </td>
                      <td className="px-3 py-2 text-right font-semibold text-ink-900">{r.entregados}</td>
                      <td className="px-3 py-2 text-right">{r.pctATiempo === null ? '—' : `${r.pctATiempo.toFixed(1)} %`}</td>
                      <td className="px-3 py-2 text-right">{formatMinutos(r.tiempoPromedioMin)}</td>
                      <td className="px-3 py-2 text-right font-semibold text-ink-900">{formatSoles(r.costo)}</td>
                      <td className="px-3 py-2 text-right">{r.averias}</td>
                      <td className="px-3 py-2 text-right">{r.bloqueos}</td>
                      <td className="px-3 py-2 text-right">
                        <button
                          type="button"
                          onClick={(e) => { e.stopPropagation(); descargarReporteDiarioCSV(r); }}
                          className="inline-flex items-center gap-1 rounded-md border border-gray-300 bg-white hover:bg-gray-50 px-2 py-1 text-[11px] font-semibold text-ink-700"
                          title="Descargar el reporte de este día en CSV"
                        >
                          <Download size={12} /> CSV
                        </button>
                      </td>
                    </tr>

                    {/* Detalle: tiempo de entrega de cada pedido de ese día */}
                    {expandido && (
                      <tr className="bg-gray-50/60">
                        <td />
                        <td colSpan={9} className="px-3 py-2.5">
                          <div className="text-[11px] font-bold text-ink-700 mb-1.5">
                            Tiempo de cada entrega ({r.entregas.length}) · desde la salida del almacén hasta la llegada al cliente
                          </div>
                          {r.entregas.length === 0 ? (
                            <p className="text-[11px] text-ink-400">Sin entregas registradas ese día.</p>
                          ) : (
                            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-1.5">
                              {r.entregas.map((e) => (
                                <div
                                  key={`${e.pedido}-${e.llegada}`}
                                  className="flex items-center justify-between gap-2 bg-white border border-gray-200 rounded px-2.5 py-1 text-[11px]"
                                >
                                  <span className="font-semibold text-ink-900 truncate">{e.pedido}</span>
                                  <span className="text-ink-400">{e.unidad}</span>
                                  <span className="font-bold text-brand-600">{formatMinutos(e.minutos)}</span>
                                </div>
                              ))}
                            </div>
                          )}
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  );
};