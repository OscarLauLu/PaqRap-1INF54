import { apiClient } from './client';
import { UnidadTransporte } from '../types';
import { INITIAL_UNIDADES } from './mockData';

export const flotaApi = {
  async listarUnidades(): Promise<UnidadTransporte[]> {
    try {
      const response = await apiClient.get<UnidadTransporte[]>('/api/flota/unidades');
      if (Array.isArray(response.data) && response.data.length > 0) {
        // Enriquecer datos con defaults para campos de UI si no vienen presentes
        return response.data.map((u) => ({
          ...u,
          nombreConductor: u.nombreConductor || 'Juan Pérez',
          turno: u.turno || '07:00–15:00',
          pedidoAsignado: u.pedidoAsignado || 'PED-2854',
        }));
      }
      return INITIAL_UNIDADES;
    } catch {
      return INITIAL_UNIDADES;
    }
  },

  async obtenerUnidad(id: number): Promise<UnidadTransporte | null> {
    try {
      const response = await apiClient.get<UnidadTransporte>(`/api/flota/unidades/${id}`);
      return response.data;
    } catch {
      return INITIAL_UNIDADES.find((u) => u.id === id) || null;
    }
  },

  async cambiarEstado(id: number, nuevoEstado: string) {
    return apiClient.put(`/api/flota/unidades/${id}/estado`, null, {
      params: { nuevoEstado },
    });
  },

  async registrarAveria(id: number, datos: { tipo: string; descripcion?: string }) {
    return apiClient.post(`/api/flota/unidades/${id}/averia`, datos);
  },
};
