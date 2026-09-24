import React, { useEffect, useState } from 'react';
import { Download, CheckCircle2, DollarSign, Wrench, Construction, Car, Truck, Bike } from 'lucide-react';
import { Card, StatTile, EmptyState } from '../ui/Card';
import { HistorialIncidentes } from '../incidentes/HistorialIncidentes';
import { useIncidentes } from '../../hooks/useIncidentes';
import { simulacionApi } from '../../api/simulacionApi';
import type { ResultadoSimulacion } from '../../types';

const formatSoles = (valor: number) => `S/ ${valor.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

const BarraEntrega: React.FC<{ etiqueta: string; icono: React.ElementType; valor: number; max: number }> = ({
  etiqueta,
  icono: Icono,
  valor,
  max,
}) => (
  <div className="flex items-center gap-3" title={etiqueta}>
    <Icono className="w-5 h-5 text-(--color-ink-700) shrink-0" aria-label={etiqueta} />
    <span className="w-20 text-xs font-semibold text-(--color-ink-500)">{etiqueta}</span>
    <div className="flex-1 bg-gray-100 rounded-full h-4 overflow-hidden">
      <div className="bg-(--color-brand-500) h-4 rounded-full" style={{ width: max > 0 ? `${(valor / max) * 100}%` : 0 }} />
    </div>
    <span className="w-10 text-right text-sm font-bold text-(--color-ink-900)">{valor}</span>
  </div>
);

export const ReportePage: React.FC = () => {
  const { incidentes, loading: cargandoIncidentes } = useIncidentes(8000);
  const [resultados, setResultados] = useState<ResultadoSimulacion[]>([]);
  const [cargando, setCargando] = useState(true);

  useEffect(() => {
    simulacionApi
      .listarResultados()
      .then(setResultados)
      .finally(() => setCargando(false));
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

  if (cargando) {
    return (
      <main className="flex-1 p-6 md:p-8">
        <p className="text-(--color-ink-400) text-sm">Cargando resultados de simulación...</p>
      </main>
    );
  }

  if (!ultimo) {
    return (
      <main className="flex-1 p-6 md:p-8">
        <Card>
          <EmptyState
            icono={CheckCircle2}
            titulo="Todavía no hay simulaciones ejecutadas"
            detalle="Corre un escenario desde Configuración para ver aquí su reporte de desempeño."
          />
        </Card>
      </main>
    );
  }

  return (
    <main className="flex-1 p-6 md:p-8 space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-bold text-(--color-ink-900)">Resumen de desempeño</h2>
          <p className="text-xs text-(--color-ink-400)">
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

      <HistorialIncidentes incidentes={incidentes} loading={cargandoIncidentes} />
    </main>
  );
};
