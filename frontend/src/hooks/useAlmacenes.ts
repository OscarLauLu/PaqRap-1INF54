import { useState, useEffect, useCallback } from 'react';
import { Almacen } from '../types';
import { almacenesApi } from '../api/almacenesApi';

export function useAlmacenes(pollIntervalMs: number = 5000) {
  const [almacenes, setAlmacenes] = useState<Almacen[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchAlmacenes = useCallback(async () => {
    try {
      const data = await almacenesApi.listarAlmacenes();
      setAlmacenes(data);
      setError(null);
    } catch (err: any) {
      setError(err.message || 'Error al cargar almacenes');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAlmacenes();
    if (pollIntervalMs > 0) {
      const interval = setInterval(fetchAlmacenes, pollIntervalMs);
      return () => clearInterval(interval);
    }
  }, [fetchAlmacenes, pollIntervalMs]);

  const cargarInventario = async (codigo: string, cantidad: number) => {
    await almacenesApi.cargarInventario(codigo, cantidad);
    await fetchAlmacenes();
  };

  const recargarDiaria = async () => {
    await almacenesApi.ejecutarRecargaDiaria();
    await fetchAlmacenes();
  };

  return {
    almacenes,
    loading,
    error,
    refresh: fetchAlmacenes,
    cargarInventario,
    recargarDiaria,
  };
}
