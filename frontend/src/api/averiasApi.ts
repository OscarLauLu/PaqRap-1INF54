import { apiClient } from './client';
import type { AveriaRegistrada, NuevaAveriaDatos } from '../types/registro';

/**
 * Registra la avería vía POST /api/flota/unidades/{codigo}/averia (FlotaController.java).
 * La hora de reincorporación y el estado los calcula el backend a partir del tipo
 * (Averia.calcularReincorporacion, RF-14) — no se envían ni se piden al usuario.
 */
export const averiasApi = {
  async registrar(datos: NuevaAveriaDatos): Promise<AveriaRegistrada> {
    const response = await apiClient.post(`/api/flota/unidades/${encodeURIComponent(datos.idUnidad)}/averia`, {
      tipo: datos.tipo,
      ubicacionX: datos.ubicacion.posX,
      ubicacionY: datos.ubicacion.posY,
      origenManual: true,
    });
    const a = response.data;
    return {
      id: a.id,
      codigo: a.codigo,
      idUnidad: datos.idUnidad,
      ubicacion: datos.ubicacion,
      tipo: datos.tipo,
      fechaHoraEvento: a.fechaHoraEvento,
      horaReincorporacion: a.horaReincorporacion,
      resuelta: a.resuelta,
    };
  },
};
