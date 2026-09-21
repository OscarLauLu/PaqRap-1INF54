import type { AveriaRegistrada, NuevaAveriaDatos } from '../types/registro';

/**
 * PASO 1: no guarda nada, solo devuelve el registro con un id.
 * PASO 4: reemplazar por un POST al backend (ej. /api/flota/unidades/{id}/averia,
 * que ya existe en flotaApi) sin tocar el componente.
 */
export const averiasApi = {
  async registrar(datos: NuevaAveriaDatos): Promise<AveriaRegistrada> {
    return { ...datos, id: `AV-${Date.now()}` };
  },
};
