import type { Almacen, UnidadTransporte } from '../types';

/**
 * Semáforo + vacío para la ocupación de vehículos (UT) y almacenes.
 * Rangos en un solo lugar (ítem B06): VACÍO 0 % · VERDE hasta 50 % · ÁMBAR hasta 80 % · ROJO más de 80 %.
 */
export type NivelSemaforo = 'VACIO' | 'VERDE' | 'AMBAR' | 'ROJO';

export const RANGOS_SEMAFORO = { verdeHasta: 50, ambarHasta: 80 };

export const NIVELES_SEMAFORO: NivelSemaforo[] = ['VACIO', 'VERDE', 'AMBAR', 'ROJO'];

export const COLOR_SEMAFORO: Record<NivelSemaforo, string> = {
  VACIO: '#94A3B8',
  VERDE: '#16A34A',
  AMBAR: '#F59E0B',
  ROJO: '#DC2626',
};

export const ETIQUETA_SEMAFORO: Record<NivelSemaforo, string> = {
  VACIO: 'Vacío',
  VERDE: 'Bajo',
  AMBAR: 'Medio',
  ROJO: 'Alto',
};

export const nivelPorPorcentaje = (pct: number | null | undefined): NivelSemaforo => {
  if (pct === null || pct === undefined || !Number.isFinite(pct) || pct <= 0) return 'VACIO';
  if (pct <= RANGOS_SEMAFORO.verdeHasta) return 'VERDE';
  if (pct <= RANGOS_SEMAFORO.ambarHasta) return 'AMBAR';
  return 'ROJO';
};

/** % de carga de un vehículo (0–100) */
export const porcentajeUnidad = (u: UnidadTransporte): number => {
  if (typeof u.porcentajeCarga === 'number' && Number.isFinite(u.porcentajeCarga)) return u.porcentajeCarga;
  return u.capacidadMaxima ? (u.cargaActual / u.capacidadMaxima) * 100 : 0;
};

/** % de ocupación de un almacén (0–100), o null si es ilimitado (central) */
export const porcentajeAlmacen = (a: Almacen): number | null => {
  if (a.tipo === 'CENTRAL' || a.stockActual === null || a.stockActual === undefined) return null;
  return a.capacidadMaxima ? (a.stockActual / a.capacidadMaxima) * 100 : null;
};

export const nivelUnidad = (u: UnidadTransporte): NivelSemaforo => nivelPorPorcentaje(porcentajeUnidad(u));

/** El central es ilimitado: nunca se llena, por eso cuenta como VERDE */
export const nivelAlmacen = (a: Almacen): NivelSemaforo => {
  const pct = porcentajeAlmacen(a);
  return pct === null ? 'VERDE' : nivelPorPorcentaje(pct);
};