import { apiClient } from './client';
import { RelojSimuladoData } from '../types';
import { INITIAL_RELOJ } from './mockData';

export const simulacionApi = {
  async consultarReloj(): Promise<RelojSimuladoData> {
    try {
      const response = await apiClient.get<RelojSimuladoData>('/api/simulacion/reloj');
      return {
        ...INITIAL_RELOJ,
        ...response.data,
      };
    } catch {
      return INITIAL_RELOJ;
    }
  },

  async iniciarSimulacion(escenario: string = 'DIA_A_DIA') {
    return apiClient.post('/api/simulacion/iniciar', null, {
      params: { escenario },
    });
  },

  async detenerSimulacion() {
    return apiClient.post('/api/simulacion/detener');
  },

  async configurar(params: Record<string, unknown>) {
    return apiClient.post('/api/simulacion/configurar', params);
  },

  async listarResultados() {
    return apiClient.get('/api/simulacion/resultados');
  },
};
