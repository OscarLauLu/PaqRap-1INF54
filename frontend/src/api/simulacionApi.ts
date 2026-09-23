import { apiClient } from './client';
import { MetricasSimulacion, ParametrosSimulacionRequest, RelojSimuladoData, ResultadoSimulacion, SemaforoCriticidad } from '../types';
import { INITIAL_RELOJ } from './mockData';

const METRICAS_VACIAS: MetricasSimulacion = {
  totalPedidosIngresados: 0,
  pedidosEnPlazo: 0,
  pedidosTarde: 0,
  costoAcumuladoTotal: 0,
  costoAuto: 0,
  costoMoto: 0,
  costoBici: 0,
  entregasAuto: 0,
  entregasMoto: 0,
  entregasBici: 0,
  bloqueosOcurridos: 0,
  averiasOcurridas: 0,
  instanteColapso: null,
  volumenPedidosColapso: null,
};

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

  async consultarMetricas(): Promise<MetricasSimulacion> {
    try {
      const response = await apiClient.get<MetricasSimulacion>('/api/simulacion/metricas');
      return { ...METRICAS_VACIAS, ...response.data };
    } catch {
      return METRICAS_VACIAS;
    }
  },

  async iniciarSimulacion(escenario: string = 'DIA_A_DIA') {
    return apiClient.post<ResultadoSimulacion>('/api/simulacion/iniciar', null, {
      params: { escenario },
    });
  },

  async detenerSimulacion() {
    return apiClient.post('/api/simulacion/detener');
  },

  async configurar(params: ParametrosSimulacionRequest) {
    return apiClient.post('/api/simulacion/configurar', params);
  },

  async listarResultados(): Promise<ResultadoSimulacion[]> {
    const response = await apiClient.get<ResultadoSimulacion[]>('/api/simulacion/resultados');
    return response.data;
  },

  async obtenerSemaforo(): Promise<SemaforoCriticidad> {
    try {
      const response = await apiClient.get<SemaforoCriticidad>('/api/mapa/semaforo');
      return response.data;
    } catch {
      // Valores por defecto reales del backend (ConfiguracionSemaforo.java).
      return { horasVerde: 12, horasAmbar: 4 };
    }
  },

  async actualizarSemaforo(horasVerde: number, horasAmbar: number) {
    return apiClient.put('/api/mapa/semaforo', null, { params: { horasVerde, horasAmbar } });
  },
};
