import { apiClient } from './client';
import { UnidadTransporte } from '../types';
import { INITIAL_UNIDADES } from './mockData';

// UnidadTransporteDTO.java: la clave es `codigo` (String), sin `id`, `turno` ni `pedidoAsignado`.
export const flotaApi = {
  async listarUnidades(): Promise<UnidadTransporte[]> {
    try {
      const response = await apiClient.get<UnidadTransporte[]>('/api/flota/unidades');
      if (Array.isArray(response.data) && response.data.length > 0) {
        return response.data;
      }
      return INITIAL_UNIDADES;
    } catch {
      return INITIAL_UNIDADES;
    }
  },

  async obtenerUnidad(codigo: string): Promise<UnidadTransporte | null> {
    try {
      const response = await apiClient.get<UnidadTransporte>(`/api/flota/unidades/${encodeURIComponent(codigo)}`);
      return response.data;
    } catch {
      return INITIAL_UNIDADES.find((u) => u.codigo === codigo) || null;
    }
  },

  async cambiarEstado(codigo: string, nuevoEstado: string) {
    return apiClient.put(`/api/flota/unidades/${encodeURIComponent(codigo)}/estado`, null, {
      params: { nuevoEstado },
    });
  },

  async registrarAveria(codigo: string, datos: { tipo: string; ubicacionX: number; ubicacionY: number }) {
    return apiClient.post(`/api/flota/unidades/${encodeURIComponent(codigo)}/averia`, {
      ...datos,
      origenManual: true,
    });
  },
};
