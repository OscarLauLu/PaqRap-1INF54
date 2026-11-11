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
  /** Acondicionamiento en el cliente (1 h por entrega, FAQ 14) */
  tiempoServicioMin?: number;
  entregada: boolean;
}

export interface RutaApi {
  id: number;
  codigo: string;
  unidadCodigo: string;
  estado: 'PLANIFICADA' | 'EN_EJECUCION' | 'COMPLETADA' | 'REPLANIFICADA' | string;
  /** Hora simulada en que el vehículo sale del almacén */
  fechaHoraGeneracion?: string;
  paradas: ParadaRutaApi[];
  // Esquinas del recorrido calculado en el backend; permiten dibujar la ruta real y no una aproximación
  camino?: Ubicacion[];
  /** Regreso al terminar: almacén con stock más cercano y hora de llegada (el camino ya lo incluye) */
  almacenRetornoCodigo?: string | null;
  horaLlegadaAlmacen?: string | null;
  /** Solo rutas de rescate: va al punto de la avería, trasvasa 30 min y luego entrega */
  rescataA?: string | null;
  partida?: Ubicacion | null;
  puntoTrasvase?: Ubicacion | null;
  llegadaTrasvase?: string | null;
  finTrasvase?: string | null;
}

export const rutasApi = {
  async listar(): Promise<RutaApi[]> {
    try {
      const response = await apiClient.get<RutaApi[]>('/api/planificacion/rutas');
      return Array.isArray(response.data) ? response.data : [];
    } catch {
      return [];
    }
  },
};