import React, { useEffect, useState } from 'react';
import { Download, CheckCircle2, DollarSign, Wrench, Construction, Car, Truck, Bike } from 'lucide-react';
import { Card, StatTile, EmptyState } from '../ui/Card';
import { HistorialIncidentes } from '../incidentes/HistorialIncidentes';
import { useIncidentes } from '../../hooks/useIncidentes';
import { simulacionApi } from '../../api/simulacionApi';
import { rutasApi, type RutaApi } from '../../api/rutasApi';
import type { ResultadoSimulacion } from '../../types';
import { ReportesDiariosCard } from './ReportesDiariosCard';

const formatSoles = (valor: number) => `S/ ${valor.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

const BarraEntrega: React.FC<{ etiqueta: string; icono: React.ElementType; valor: number; max: number }> = ({
  etiqueta,
  icono: Icono,
  valor,
  max,
}) => (
  <div className="flex items-center gap-3" title={etiqueta}>
    <Icono className="w-5 h-5 text-ink-700 shrink-0" aria-label={etiqueta} />
    <span className="w-20 text-xs font-semibold text-ink-500">{etiqueta}</span>
    <div className="flex-1 bg-gray-100 rounded-full h-4 overflow-hidden">
      <div className="bg-brand-500 h-4 rounded-full" style={{ width: max > 0 ? `${(valor / max) * 100}%` : 0 }} />
    </div>
    <span className="w-10 text-right text-sm font-bold text-ink-900">{valor}</span>
  </div>
);

export const ReportePage: React.FC = () => {
  const { incidentes, loading: cargandoIncidentes } = useIncidentes(8000);
  const [resultados, setResultados] = useState<ResultadoSimulacion[]>([]);
  const [rutas, setRutas] = useState<RutaApi[]>([]);
  const [rutaExpandida, setRutaExpandida] = useState<number | null>(null);
  const [cargando, setCargando] = useState(true);

  useEffect(() => {
    simulacionApi
      .listarResultados()
      .then(setResultados)
      .finally(() => setCargando(false));
    rutasApi
      .listar()
      .then(setRutas)
      .catch(() => {});
  }, []);

  const ultimo = resultados.length > 0 ? resultados[resultados.length - 1] : null;
  const maxEntregas = ultimo ? Math.max(ultimo.entregasAuto, ultimo.entregasMoto, ultimo.entregasBicicleta, 1) : 1;

  const exportarCSV = () => {
    if (!ultimo) return;
    const filas = [
      ['codigo', 'escenario', 'porcentajeCumplimiento', 'costoTotalOperacion', 'totalAveriasOcurridas', 'totalBloqueosOcurridos'],
      [ultimo.codigo, ultimo.escenario, String(ultimo.porcentajeCumplimiento), String(ultimo.costoTotalOperacion), String(ultimo.totalAveriasOcurridas), String(ultimo.totalBloqueosOcurridos)],
    ];
    const csv = filas.map((f) => f.join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `reporte-${ultimo.codigo}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const exportarPlanificacionCSV = () => {
    if (rutas.length === 0) return;
    const encabezados = ['Ruta', 'Unidad', 'Estado', 'Salida', 'LlegadaAlmacen', 'OrdenParada', 'Pedido', 'DestinoX', 'DestinoY', 'HoraLlegadaParada', 'Entregada'];
    const filas: string[][] = [encabezados];
    rutas.forEach((r) => {
      if (!r.paradas || r.paradas.length === 0) {
        filas.push([r.codigo, r.unidadCodigo, r.estado, r.fechaHoraGeneracion || '', r.horaLlegadaAlmacen || '', '', '', '', '', '', '']);
      } else {
        r.paradas.forEach((p) => {
          filas.push([
            r.codigo,
            r.unidadCodigo,
            r.estado,
            r.fechaHoraGeneracion || '',
            r.horaLlegadaAlmacen || '',
            String(p.orden),
            p.codigoPedido,
            String(p.destino?.posX ?? ''),
            String(p.destino?.posY ?? ''),
            p.horaEstimadaLlegada || '',
            p.entregada ? 'SI' : 'NO',
          ]);
        });
      }
    });
    const csv = filas.map((f) => f.join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `planificacion-${ultimo?.codigo || 'estable'}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  if (cargando) {
    return (
      <main className="flex-1 p-6 md:p-8">
        <p className="text-ink-400 text-sm">Cargando resultados de simulación...</p>
      </main>
    );
  }

  if (!ultimo) {
    return (
      <main className="flex-1 p-6 md:p-8 space-y-6">
        <Card>
          <EmptyState
            icono={CheckCircle2}
            titulo="Todavía no hay simulaciones ejecutadas"
            detalle="Corre un escenario desde Configuración para ver aquí su reporte de desempeño."
          />
        </Card>

        {/* Reportes diarios de la operación día a día (G09): no dependen de las simulaciones */}
        <ReportesDiariosCard />
      </main>
    );
  }

  return (
    <main className="flex-1 p-6 md:p-8 space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-bold text-ink-900">Resumen de desempeño</h2>
          <p className="text-xs text-ink-400">
            {ultimo.codigo} · {ultimo.escenario.replace(/_/g, ' ')} · Fin: {ultimo.fechaHoraFin ? new Date(ultimo.fechaHoraFin).toLocaleString('es-PE') : '—'}
          </p>
        </div>
        <button
          type="button"
          onClick={exportarCSV}
          className="flex items-center gap-2 rounded-xl bg-gray-500 hover:bg-gray-600 text-white font-semibold px-5 py-2.5 text-sm transition-colors"
        >
          <Download className="w-4 h-4" />
          Exportar
        </button>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <StatTile etiqueta="A tiempo" valor={`${ultimo.porcentajeCumplimiento.toFixed(1)}%`} tono={ultimo.porcentajeCumplimiento >= 90 ? 'verde' : 'ambar'} icono={CheckCircle2} />
        <StatTile etiqueta="Costo total" valor={formatSoles(ultimo.costoTotalOperacion)} tono="azul" icono={DollarSign} />
        <StatTile etiqueta="Averías" valor={ultimo.totalAveriasOcurridas} tono="rojo" icono={Wrench} />
        <StatTile etiqueta="Bloqueos" valor={ultimo.totalBloqueosOcurridos} tono="ambar" icono={Construction} />
      </div>

      {ultimo.instanteColapso && (
        <Card className="border-red-300 bg-red-50">
          <p className="text-sm font-bold text-red-700">Colapso logístico detectado</p>
          <p className="text-xs text-red-600 mt-1">
            En {new Date(ultimo.instanteColapso).toLocaleString('es-PE')}, con {ultimo.volumenPedidosColapso ?? '—'} pedido(s)
            activos en el sistema en ese instante — esa es la carga que el algoritmo logró sostener antes de fallar.
          </p>
        </Card>
      )}

      <Card titulo="Entregas por tipo de vehículo">
        <div className="space-y-4">
          <BarraEntrega etiqueta="Autos" icono={Car} valor={ultimo.entregasAuto} max={maxEntregas} />
          <BarraEntrega etiqueta="Motos" icono={Truck} valor={ultimo.entregasMoto} max={maxEntregas} />
          <BarraEntrega etiqueta="Bicicletas" icono={Bike} valor={ultimo.entregasBicicleta} max={maxEntregas} />
        </div>
      </Card>

      {/* Última Planificación Estable (Sí3) */}
      <Card titulo="Última Planificación Estable de Rutas (Algoritmo ACO)">
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <p className="text-xs text-ink-500">
              Planificación consolidada de rutas para la flota ({rutas.length} rutas registradas).
            </p>
            {rutas.length > 0 && (
              <button
                type="button"
                onClick={exportarPlanificacionCSV}
                className="flex items-center gap-1.5 rounded-lg border border-gray-300 bg-white hover:bg-gray-50 text-ink-700 px-3 py-1.5 text-xs font-semibold transition-colors"
              >
                <Download size={13} /> Exportar Plan (CSV)
              </button>
            )}
          </div>

          {rutas.length === 0 ? (
            <p className="text-sm text-ink-400 py-3 text-center">No se encontraron rutas registradas para esta corrida.</p>
          ) : (
            <div className="overflow-x-auto rounded-lg border border-gray-200">
              <table className="w-full text-xs text-left">
                <thead className="bg-gray-50 border-b border-gray-200 text-ink-500 uppercase font-semibold">
                  <tr>
                    <th className="px-3 py-2">Ruta</th>
                    <th className="px-3 py-2">Unidad</th>
                    <th className="px-3 py-2">Estado</th>
                    <th className="px-3 py-2">Salida</th>
                    <th className="px-3 py-2">Paradas</th>
                    <th className="px-3 py-2">Retorno Almacén</th>
                    <th className="px-3 py-2 text-right">Detalle</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {rutas.map((r) => {
                    const expandida = rutaExpandida === r.id;
                    const paradas = r.paradas || [];
                    return (
                      <React.Fragment key={r.id}>
                        <tr className="hover:bg-gray-50/80 transition-colors">
                          <td className="px-3 py-2 font-bold text-ink-900">{r.codigo}</td>
                          <td className="px-3 py-2 font-semibold text-brand-600">{r.unidadCodigo}</td>
                          <td className="px-3 py-2">
                            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-blue-50 text-blue-700 border border-blue-200">
                              {r.estado}
                            </span>
                          </td>
                          <td className="px-3 py-2 text-ink-500">
                            {r.fechaHoraGeneracion ? new Date(r.fechaHoraGeneracion).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' }) : '—'}
                          </td>
                          <td className="px-3 py-2 font-medium text-ink-700">{paradas.length} paradas</td>
                          <td className="px-3 py-2 text-ink-500">
                            {r.horaLlegadaAlmacen ? new Date(r.horaLlegadaAlmacen).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' }) : '—'}
                          </td>
                          <td className="px-3 py-2 text-right">
                            <button
                              type="button"
                              onClick={() => setRutaExpandida(expandida ? null : r.id)}
                              className="text-brand-500 hover:text-brand-700 font-bold text-xs"
                            >
                              {expandida ? 'Ocultar' : 'Ver paradas'}
                            </button>
                          </td>
                        </tr>
                        {expandida && (
                          <tr className="bg-gray-50/60">
                            <td colSpan={7} className="px-4 py-2.5">
                              <div className="space-y-1">
                                <span className="font-bold text-[11px] text-ink-700">Secuencia de entregas:</span>
                                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-2 pt-1">
                                  {paradas.map((p) => (
                                    <div key={p.id || p.codigoPedido} className="flex justify-between items-center bg-white border border-gray-200 rounded px-2.5 py-1 text-[11px]">
                                      <span className="font-semibold text-ink-900">
                                        #{p.orden} · {p.codigoPedido}
                                      </span>
                                      <span className="text-gray-400">({p.destino?.posX},{p.destino?.posY})</span>
                                      <span className={p.entregada ? 'text-green-600 font-bold' : 'text-amber-600 font-semibold'}>
                                        {p.entregada ? 'Entregada' : 'Pendiente'}
                                      </span>
                                    </div>
                                  ))}
                                </div>
                              </div>
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
        </div>
      </Card>

      {/* Reportes diarios de la operación día a día (G09) */}
      <ReportesDiariosCard />

      <HistorialIncidentes incidentes={incidentes} loading={cargandoIncidentes} />
    </main>
  );
};
