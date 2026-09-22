import { useCallback, useEffect, useState } from 'react';
import { mapaApi } from '../api/mapaApi';
import type { AveriaVial, BloqueoVial } from '../types';

/**
 * Lee los bloqueos y averías del motor (mismo endpoint que usa el mapa).
 * Si el backend no responde, conserva lo último recibido.
 */
export function useIncidenciasMapa(pollIntervalMs: number = 3000) {
  const [bloqueos, setBloqueos] = useState<BloqueoVial[]>([]);
  const [averias, setAverias] = useState<AveriaVial[]>([]);

  const fetchDatos = useCallback(async () => {
    const data = await mapaApi.obtenerMapaOperaciones();
    if (data.bloqueos) setBloqueos(data.bloqueos);
    if (data.averias) setAverias(data.averias);
  }, []);

  useEffect(() => {
    fetchDatos();
    if (pollIntervalMs > 0) {
      const interval = setInterval(fetchDatos, pollIntervalMs);
      return () => clearInterval(interval);
    }
  }, [fetchDatos, pollIntervalMs]);

  return { bloqueos, averias };
}
