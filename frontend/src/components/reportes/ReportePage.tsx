import React, { useEffect, useMemo, useState } from 'react';
import { Download, CheckCircle2, DollarSign, Wrench, Construction, Car, Truck, Bike, Search, ChevronLeft, ChevronRight, X } from 'lucide-react';
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

  // Búsqueda y paginación de rutas
  const [busquedaRuta, setBusquedaRuta] = useState('');
  const [paginaRuta, setPaginaRuta] = useState(1);
  const RUTAS_POR_PAGINA = 8;

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
      [
        'codigo', 'escenario', 'fechaHoraInicio', 'fechaHoraFin', 'tiempoEjecucionRealSegundos',
        'instanteColapso', 'volumenPedidosColapso',
        'totalPedidos', 'pedidosEntregadosEnPlazo', 'pedidosEntregadosTarde', 'porcentajeCumplimiento',
        'costoTotalOperacion', 'costoTotalAuto', 'costoTotalMoto', 'costoTotalBicicleta',
        'entregasAuto', 'entregasMoto', 'entregasBicicleta',
        'totalBloqueosOcurridos', 'totalAveriasOcurridas',
      ],
      [
        ultimo.codigo, ultimo.escenario, ultimo.fechaHoraInicio ?? '', ultimo.fechaHoraFin ?? '',
        ultimo.tiempoEjecucionRealSegundos ?? '',
        ultimo.instanteColapso ?? '', ultimo.volumenPedidosColapso ?? '',
        ultimo.totalPedidos, ultimo.pedidosEntregadosEnPlazo, ultimo.pedidosEntregadosTarde, ultimo.porcentajeCumplimiento,
        ultimo.costoTotalOperacion, ultimo.costoTotalAuto, ultimo.costoTotalMoto, ultimo.costoTotalBicicleta,
        ultimo.entregasAuto, ultimo.entregasMoto, ultimo.entregasBicicleta,
        ultimo.totalBloqueosOcurridos, ultimo.totalAveriasOcurridas,
      ].map(String),
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

  // Filtrado y paginación de la búsqueda de rutas
  const rutasFiltradas = useMemo(() => {
    const q = busquedaRuta.trim().toLowerCase();
    if (!q) return rutas;
    return rutas.filter((r) => {
      if (r.codigo.toLowerCase().includes(q)) return true;
      if (r.unidadCodigo.toLowerCase().includes(q)) return true;
      if (r.estado.toLowerCase().includes(q)) return true;
      if (r.paradas?.some((p) => p.codigoPedido.toLowerCase().includes(q))) return true;
      return false;
    });
  }, [rutas, busquedaRuta]);

  const totalPaginasRuta = Math.max(1, Math.ceil(rutasFiltradas.length / RUTAS_POR_PAGINA));
  const paginaValida = Math.min(Math.max(1, paginaRuta), totalPaginasRuta);

  const rutasPaginadas = useMemo(() => {
    const inicio = (paginaValida - 1) * RUTAS_POR_PAGINA;
    return rutasFiltradas.slice(inicio, inicio + RUTAS_POR_PAGINA);
  }, [rutasFiltradas, paginaValida]);

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

      {/* Última Planificación Estable con Búsqueda y Paginación */}
      <Card titulo="Última Planificación Estable de Rutas (Algoritmo ACO)">
        <div className="space-y-3">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
            <p className="text-xs text-ink-500">
              Planificación consolidada de rutas para la flota ({rutas.length} rutas registradas
              {busquedaRuta.trim() ? `, ${rutasFiltradas.length} encontradas` : ''}).
            </p>
            {rutas.length > 0 && (
              <button
                type="button"
                onClick={exportarPlanificacionCSV}
                className="flex items-center gap-1.5 self-start sm:self-auto rounded-lg border border-gray-300 bg-white hover:bg-gray-50 text-ink-700 px-3 py-1.5 text-xs font-semibold transition-colors"
              >
                <Download size={13} /> Exportar Plan (CSV)
              </button>
            )}
          </div>

          {/* Barra de Búsqueda de Rutas */}
          {rutas.length > 0 && (
            <div className="flex items-center gap-2">
              <div className="relative flex-1">
                <input
                  type="text"
                  placeholder="Buscar por código de ruta, unidad (AUTO-01), pedido (PED-0001) o estado..."
                  value={busquedaRuta}
                  onChange={(e) => {
                    setBusquedaRuta(e.target.value);
                    setPaginaRuta(1);
                  }}
                  className="w-full bg-white border border-gray-300 rounded-lg pl-8 pr-7 py-1.5 text-xs text-ink-900 placeholder-ink-400 focus:outline-none focus:border-brand-500"
                />
                <Search size={14} className="absolute left-2.5 top-2 text-ink-400" />
                {busquedaRuta && (
                  <button
                    type="button"
                    onClick={() => {
                      setBusquedaRuta('');
                      setPaginaRuta(1);
                    }}
                    className="absolute right-2 top-2 text-gray-400 hover:text-gray-600"
                    title="Limpiar búsqueda"
                  >
                    <X size={14} />
                  </button>
                )}
              </div>
            </div>
          )}

          {rutas.length === 0 ? (
            <p className="text-sm text-ink-400 py-3 text-center">No se encontraron rutas registradas para esta corrida.</p>
          ) : rutasFiltradas.length === 0 ? (
            <div className="p-4 text-center border border-dashed border-gray-200 rounded-lg space-y-1">
              <p className="text-xs font-semibold text-ink-600">No se encontraron rutas que coincidan con &ldquo;{busquedaRuta}&rdquo;.</p>
              <button
                type="button"
                onClick={() => {
                  setBusquedaRuta('');
                  setPaginaRuta(1);
                }}
                className="text-xs font-bold text-brand-600 hover:underline"
              >
                Limpiar búsqueda
              </button>
            </div>
          ) : (
            <>
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
                    {rutasPaginadas.map((r) => {
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

              {/* Controles de Paginación */}
              <div className="flex flex-col sm:flex-row items-center justify-between gap-2 pt-2 px-1 text-xs text-ink-500">
                <span>
                  Mostrando{' '}
                  <strong className="text-ink-800">
                    {(paginaValida - 1) * RUTAS_POR_PAGINA + 1}
                  </strong>{' '}
                  a{' '}
                  <strong className="text-ink-800">
                    {Math.min(paginaValida * RUTAS_POR_PAGINA, rutasFiltradas.length)}
                  </strong>{' '}
                  de <strong className="text-ink-800">{rutasFiltradas.length}</strong> rutas
                </span>

                <div className="flex items-center gap-1.5">
                  <button
                    type="button"
                    onClick={() => setPaginaRuta((p) => Math.max(1, p - 1))}
                    disabled={paginaValida <= 1}
                    className="flex items-center gap-1 px-2.5 py-1 rounded border border-gray-300 bg-white hover:bg-gray-50 text-ink-700 disabled:opacity-40 disabled:cursor-not-allowed font-medium transition-colors"
                  >
                    <ChevronLeft size={13} /> Anterior
                  </button>

                  <div className="flex items-center gap-1 px-1">
                    {Array.from({ length: totalPaginasRuta }, (_, i) => i + 1)
                      .filter((p) => p === 1 || p === totalPaginasRuta || Math.abs(p - paginaValida) <= 1)
                      .reduce<(number | string)[]>((acc, p, idx, arr) => {
                        if (idx > 0 && p - (arr[idx - 1] as number) > 1) acc.push('...');
                        acc.push(p);
                        return acc;
                      }, [])
                      .map((p, idx) =>
                        typeof p === 'string' ? (
                          <span key={`dots-${idx}`} className="px-1 text-gray-400">…</span>
                        ) : (
                          <button
                            key={p}
                            type="button"
                            onClick={() => setPaginaRuta(p)}
                            className={`w-6 h-6 rounded text-xs font-bold transition-colors ${
                              paginaValida === p
                                ? 'bg-brand-500 text-white'
                                : 'bg-white border border-gray-200 text-ink-700 hover:bg-gray-100'
                            }`}
                          >
                            {p}
                          </button>
                        )
                      )}
                  </div>

                  <button
                    type="button"
                    onClick={() => setPaginaRuta((p) => Math.min(totalPaginasRuta, p + 1))}
                    disabled={paginaValida >= totalPaginasRuta}
                    className="flex items-center gap-1 px-2.5 py-1 rounded border border-gray-300 bg-white hover:bg-gray-50 text-ink-700 disabled:opacity-40 disabled:cursor-not-allowed font-medium transition-colors"
                  >
                    Siguiente <ChevronRight size={13} />
                  </button>
                </div>
              </div>
            </>
          )}
        </div>
      </Card>

      {/* Reportes diarios de la operación día a día (G09) */}
      <ReportesDiariosCard />

      <HistorialIncidentes incidentes={incidentes} loading={cargandoIncidentes} />
    </main>
  );
};
