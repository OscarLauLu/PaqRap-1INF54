import { apiClient } from './client';
import { Almacen } from '../types';
import { INITIAL_ALMACENES } from './mockData';

// AlmacenDTO.java: la clave es `codigo` (String), sin `id`.
export const almacenesApi = {
  async listarAlmacenes(): Promise<Almacen[]> {
    try {
      const response = await apiClient.get<Almacen[]>('/api/almacenes');
      if (Array.isArray(response.data) && response.data.length > 0) {
        return response.data;
      }
      return INITIAL_ALMACENES;
    } catch {
      return INITIAL_ALMACENES;
    }
  },

  async obtenerAlmacen(codigo: string): Promise<Almacen | null> {
    try {
      const response = await apiClient.get<Almacen>(`/api/almacenes/${encodeURIComponent(codigo)}`);
      return response.data;
    } catch {
      return INITIAL_ALMACENES.find((a) => a.codigo === codigo) || null;
    }
  },

  async cargarInventario(codigo: string, cantidad: number) {
    const response = await apiClient.post(`/api/almacenes/${encodeURIComponent(codigo)}/carga`, null, {
      params: { cantidad },
    });
    return response.data;
  },

  async ejecutarRecargaDiaria() {
    const response = await apiClient.post('/api/almacenes/recarga-diaria');
    return response.data;
  },
};
