import { useState, useEffect, useCallback } from 'react';
import { Pedido } from '../types';
import { mapaApi } from '../api/mapaApi';
import { INITIAL_PEDIDOS } from '../api/mockData';

export function useMapaOperaciones(pollIntervalMs: number = 4000) {
  const [pedidos, setPedidos] = useState<Pedido[]>(INITIAL_PEDIDOS);
  const [loading, setLoading] = useState<boolean>(true);

  const fetchMapa = useCallback(async () => {
    try {
      const pedidosData = await mapaApi.obtenerPedidos();
      setPedidos(pedidosData);
    } catch {
      setPedidos(INITIAL_PEDIDOS);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchMapa();
    if (pollIntervalMs > 0) {
      const interval = setInterval(fetchMapa, pollIntervalMs);
      return () => clearInterval(interval);
    }
  }, [fetchMapa, pollIntervalMs]);

  return {
    pedidos,
    loading,
    refresh: fetchMapa,
  };
}
