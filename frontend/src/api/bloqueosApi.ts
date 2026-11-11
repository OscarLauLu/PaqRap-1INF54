import { apiClient } from './client';
import type { BloqueoVial } from '../types';
import type { NuevoBloqueoDatos } from '../types/registro';

/**
 * Dos flujos distintos y separados en el backend (RedVialController.java):
 * - subirArchivo: carga masiva desde un archivo mensual de bloqueos (aaaamm.bloqueadas).
 * - registrarManual: alta de un único bloqueo desde el formulario.
 */
export const bloqueosApi = {
  async listar(): Promise<BloqueoVial[]> {
    try {
      const response = await apiClient.get<BloqueoVial[]>('/api/redvial/bloqueos');
      return Array.isArray(response.data) ? response.data : [];
    } catch {
      return [];
    }
  },

  async subirArchivo(archivo: File): Promise<BloqueoVial[]> {
    const formData = new FormData();
    formData.append('archivo', archivo);
    const response = await apiClient.post<BloqueoVial[]>('/api/redvial/bloqueos/subir-archivo', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },

  async registrarManual(datos: NuevoBloqueoDatos): Promise<BloqueoVial> {
    const response = await apiClient.post<BloqueoVial>('/api/redvial/bloqueos', {
      fechaHoraInicio: datos.inicio,
      fechaHoraFin: datos.fin,
      coordenadasNodos: datos.coordenadasNodos,
    });
    return response.data;
  },
};
