import { apiClient } from './client';
import { Almacen } from '../types';
import { INITIAL_ALMACENES } from './mockData';

export const almacenesApi = {
  async listarAlmacenes(): Promise<Almacen[]> {
    try {
      const response = await apiClient.get<Almacen[]>('/api/almacenes');
      return response.data;
    } catch {
      // Fallback a datos simulados si el backend no está disponible
      return INITIAL_ALMACENES;
    }
  },

  async obtenerAlmacen(id: number): Promise<Almacen | null> {
    try {
      const response = await apiClient.get<Almacen>(`/api/almacenes/${id}`);
      return response.data;
    } catch {
      return INITIAL_ALMACENES.find((a) => a.id === id) || null;
    }
  },

  async cargarInventario(id: number, cantidad: number) {
    const response = await apiClient.post(`/api/almacenes/${id}/carga`, null, {
      params: { cantidad },
    });
    return response.data;
  },

  async ejecutarRecargaDiaria() {
    const response = await apiClient.post('/api/almacenes/recarga-diaria');
    return response.data;
  },
};
