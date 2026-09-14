import { apiClient } from './client';
import { MapaOperacionesData, Pedido, AlertaItem, IncidenteItem } from '../types';
import { INITIAL_PEDIDOS, INITIAL_INCIDENTES } from './mockData';

export const mapaApi = {
  async obtenerMapaOperaciones(): Promise<Partial<MapaOperacionesData>> {
    try {
      const response = await apiClient.get<MapaOperacionesData>('/api/mapa/operaciones');
      return response.data;
    } catch {
      return {};
    }
  },

  async obtenerPedidos(): Promise<Pedido[]> {
    try {
      const response = await apiClient.get<Pedido[]>('/api/mapa/pedidos');
      return response.data;
    } catch {
      return INITIAL_PEDIDOS;
    }
  },

  async obtenerIncidencias(): Promise<IncidenteItem[]> {
    try {
      const response = await apiClient.get<{ bloqueos: unknown[]; averias: unknown[] }>('/api/mapa/incidencias');
      const { bloqueos = [], averias = [] } = response.data;
      if (bloqueos.length === 0 && averias.length === 0) {
        return INITIAL_INCIDENTES;
      }
      // Mapear respuesta del backend al formato de la tabla
      const list: IncidenteItem[] = [];
      let counter = 1;
      averias.forEach((a: any) => {
        list.push({
          id: `av-${a.id || counter}`,
          numero: counter++,
          tipo: 'averia',
          evento: `Avería: ${a.tipo?.replace('_', ' ') || 'Tipo 1'}`,
          ubicacion: a.ubicacionFalla ? `(${a.ubicacionFalla.x}, ${a.ubicacionFalla.y}) km` : '(40, 15) km',
          impacto: a.impacto || `${a.unidad?.codigo || 'Unidad'} detenida`,
          dia: 1,
          hora: a.fechaHoraEvento ? new Date(a.fechaHoraEvento).toLocaleTimeString() : '19:01:02',
          estado: a.resuelta ? 'Resuelta' : 'En curso',
        });
      });
      bloqueos.forEach((b: any) => {
        list.push({
          id: `bl-${b.id || counter}`,
          numero: counter++,
          tipo: 'bloqueo',
          evento: 'Bloqueo: Restricción',
          ubicacion: b.coordenadasNodos || '(30,15) → (35,15)',
          impacto: b.impacto || 'Ruta afectada',
          dia: 1,
          hora: b.fechaHoraInicio ? new Date(b.fechaHoraInicio).toLocaleTimeString() : '20:01:02',
          estado: b.activo ? 'En curso' : 'Resuelta',
        });
      });
      return list;
    } catch {
      return INITIAL_INCIDENTES;
    }
  },

  async listarAlertas(): Promise<AlertaItem[]> {
    try {
      const response = await apiClient.get<AlertaItem[]>('/api/mapa/alertas?soloNoAtendidas=true');
      return response.data;
    } catch {
      return [];
    }
  },

  async marcarAlertaAtendida(id: number): Promise<void> {
    await apiClient.put(`/api/mapa/alertas/${id}/atender`);
  },
};
