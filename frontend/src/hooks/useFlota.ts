import { useState, useEffect, useCallback } from 'react';
import { UnidadTransporte } from '../types';
import { flotaApi } from '../api/flotaApi';

export function useFlota(pollIntervalMs: number = 4000) {
  const [unidades, setUnidades] = useState<UnidadTransporte[]>([]);
  const [selectedUnidadCodigo, setSelectedUnidadCodigo] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchUnidades = useCallback(async () => {
    try {
      const data = await flotaApi.listarUnidades();
      setUnidades(data);
      setError(null);
    } catch (err: any) {
      setError(err.message || 'Error al obtener flota');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchUnidades();
    if (pollIntervalMs > 0) {
      const interval = setInterval(fetchUnidades, pollIntervalMs);
      return () => clearInterval(interval);
    }
  }, [fetchUnidades, pollIntervalMs]);

  const selectedUnidad = unidades.find((u) => u.codigo === selectedUnidadCodigo) || null;

  return {
    unidades,
    selectedUnidad,
    selectedUnidadCodigo,
    setSelectedUnidadCodigo,
    loading,
    error,
    refresh: fetchUnidades,
  };
}
