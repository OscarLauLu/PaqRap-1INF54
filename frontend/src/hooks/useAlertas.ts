import { useState, useEffect, useCallback } from 'react';
import { AlertaItem } from '../types';
import { mapaApi } from '../api/mapaApi';

export function useAlertas(pollIntervalMs: number = 6000) {
  const [alertas, setAlertas] = useState<AlertaItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  const fetchAlertas = useCallback(async () => {
    try {
      const data = await mapaApi.listarAlertas();
      setAlertas(data);
    } catch {
      // Ignorar errores menores de alertas
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAlertas();
    if (pollIntervalMs > 0) {
      const interval = setInterval(fetchAlertas, pollIntervalMs);
      return () => clearInterval(interval);
    }
  }, [fetchAlertas, pollIntervalMs]);

  const atenderAlerta = async (id: number) => {
    await mapaApi.marcarAlertaAtendida(id);
    setAlertas((prev) => prev.filter((a) => a.id !== id));
  };

  return {
    alertas,
    totalAlertas: alertas.length,
    loading,
    refresh: fetchAlertas,
    atenderAlerta,
  };
}
