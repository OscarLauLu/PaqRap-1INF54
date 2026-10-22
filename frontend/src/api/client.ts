import axios from 'axios';

export const API_BASE_URL = import.meta.env.VITE_API_URL || '';

/**
 * Hay dos backends independientes:
 * - 'operacion'  → Operación día a día (siempre encendida) y Registro.
 * - 'simulacion' → Simulación 5D, colapso, Configuración y Reportes.
 * App.tsx indica cuál usar según la pantalla, y todas las llamadas se envían a ese.
 */
export type Backend = 'operacion' | 'simulacion';

let backendActual: Backend = 'operacion';

export const setBackend = (backend: Backend) => {
  backendActual = backend;
};

export const getBackend = (): Backend => backendActual;

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 5000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Antes de cada llamada: si la pantalla actual es de simulación, la ruta /api/... pasa a ser
// /sim/api/..., y el proxy de Vite la envía al backend de simulación (puerto 8081)
apiClient.interceptors.request.use((config) => {
  if (backendActual === 'simulacion' && config.url?.startsWith('/api/')) {
    config.url = `/sim${config.url}`;
  }
  return config;
});
