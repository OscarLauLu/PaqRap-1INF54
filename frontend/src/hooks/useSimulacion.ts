import { useState, useEffect, useCallback } from 'react';
import { RelojSimuladoData } from '../types';
import { simulacionApi } from '../api/simulacionApi';
import { INITIAL_RELOJ } from '../api/mockData';

export function useSimulacion(pollIntervalMs: number = 3000) {
  const [reloj, setReloj] = useState<RelojSimuladoData>(INITIAL_RELOJ);
  const [loading, setLoading] = useState<boolean>(true);
  const [iniciando, setIniciando] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const fetchReloj = useCallback(async () => {
    try {
      const data = await simulacionApi.consultarReloj();
      setReloj(data);
      setError(null);
    } catch (err: any) {
      setError(err.message || 'Error al consultar reloj simulado');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchReloj();
    if (pollIntervalMs > 0) {
      const interval = setInterval(fetchReloj, pollIntervalMs);
      return () => clearInterval(interval);
    }
  }, [fetchReloj, pollIntervalMs]);

  const iniciar = async (escenario: string = 'DIA_A_DIA') => {
    setIniciando(true);
    try {
      await simulacionApi.iniciarSimulacion(escenario);
      await fetchReloj();
    } finally {
      setIniciando(false);
    }
  };

  const detener = async () => {
    await simulacionApi.detenerSimulacion();
    setReloj((prev) => ({ ...prev, estadoEjecucion: 'DETENIDA' }));
  };

  const finalizarSimulacion = async () => {
    try {
      await simulacionApi.detenerSimulacion();
    } catch (e) {
      console.warn('Simulación finalizada en modo local:', e);
    }
    setReloj((prev) => ({
      ...prev,
      estadoEjecucion: 'FINALIZADA',
    }));
  };

  return {
    reloj,
    loading,
    iniciando,
    error,
    refresh: fetchReloj,
    iniciar,
    detener,
    finalizarSimulacion,
  };
}
