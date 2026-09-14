import { useState, useEffect, useCallback } from 'react';
import { IncidenteItem } from '../types';
import { mapaApi } from '../api/mapaApi';

export function useIncidentes(pollIntervalMs: number = 5000) {
  const [incidentes, setIncidentes] = useState<IncidenteItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchIncidentes = useCallback(async () => {
    try {
      const data = await mapaApi.obtenerIncidencias();
      setIncidentes(data);
      setError(null);
    } catch (err: any) {
      setError(err.message || 'Error al obtener incidencias');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchIncidentes();
    if (pollIntervalMs > 0) {
      const interval = setInterval(fetchIncidentes, pollIntervalMs);
      return () => clearInterval(interval);
    }
  }, [fetchIncidentes, pollIntervalMs]);

  return {
    incidentes,
    loading,
    error,
    refresh: fetchIncidentes,
  };
}
