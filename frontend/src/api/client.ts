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

const SESSION_STORAGE_KEY = 'paqrap_device_session_id';

export const getDeviceSessionId = (): string => {
  try {
    let id = localStorage.getItem(SESSION_STORAGE_KEY);
    if (!id) {
      id = typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID() : 'dev-' + Math.random().toString(36).substring(2, 10);
      localStorage.setItem(SESSION_STORAGE_KEY, id);
    }
    return id;
  } catch {
    return 'dev-default-session';
  }
};

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 5000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Antes de cada llamada: si la pantalla actual es de simulación, la ruta /api/... pasa a ser
// /sim/api/..., y el proxy de Vite la envía al backend de simulación (puerto 8081).
apiClient.interceptors.request.use((config) => {
  if (backendActual === 'simulacion' && config.url?.startsWith('/api/')) {
    config.url = `/sim${config.url}`;
  }
  return config;
});
