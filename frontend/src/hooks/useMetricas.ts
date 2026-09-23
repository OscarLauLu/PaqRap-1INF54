import { useCallback, useEffect, useState } from 'react';
import { MetricasSimulacion } from '../types';
import { simulacionApi } from '../api/simulacionApi';

const VACIAS: MetricasSimulacion = {
  totalPedidosIngresados: 0,
  pedidosEnPlazo: 0,
  pedidosTarde: 0,
  costoAcumuladoTotal: 0,
  costoAuto: 0,
  costoMoto: 0,
  costoBici: 0,
  entregasAuto: 0,
  entregasMoto: 0,
  entregasBici: 0,
  bloqueosOcurridos: 0,
  averiasOcurridas: 0,
  instanteColapso: null,
  volumenPedidosColapso: null,
};

export function useMetricas(pollIntervalMs: number = 5000) {
  const [metricas, setMetricas] = useState<MetricasSimulacion>(VACIAS);
  const [loading, setLoading] = useState(true);

  const fetchMetricas = useCallback(async () => {
    try {
      const data = await simulacionApi.consultarMetricas();
      setMetricas(data);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchMetricas();
    if (pollIntervalMs > 0) {
      const interval = setInterval(fetchMetricas, pollIntervalMs);
      return () => clearInterval(interval);
    }
  }, [fetchMetricas, pollIntervalMs]);

  return { metricas, loading, refresh: fetchMetricas };
}
