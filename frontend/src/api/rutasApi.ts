import { apiClient } from './client';
import type { Ubicacion } from '../types';

// Coincide con la respuesta de GET /api/planificacion/rutas (PlanificacionController)
export interface ParadaRutaApi {
  id: number;
  orden: number;
  pedidoId: number;
  codigoPedido: string;
  destino: Ubicacion;
  horaEstimadaLlegada: string;
  entregada: boolean;
}

export interface RutaApi {
  id: number;
  codigo: string;
  unidadCodigo: string;
  estado: 'PLANIFICADA' | 'EN_EJECUCION' | 'COMPLETADA' | 'REPLANIFICADA' | string;
  paradas: ParadaRutaApi[];
  // Esquinas del recorrido calculado en el backend; permiten dibujar la ruta real y no una aproximación
  camino?: Ubicacion[];
}

export const rutasApi = {
  async listar(): Promise<RutaApi[]> {
    try {
      const response = await apiClient.get<RutaApi[]>('/api/planificacion/rutas');
      return response.data;
    } catch {
      return [];
    }
  },
};