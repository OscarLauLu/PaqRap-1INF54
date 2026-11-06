import type { Almacen, UnidadTransporte } from '../types';

/**
 * Qué se muestra en el mapa. Desde el panel derecho se puede ocultar:
 * - cada tipo de vehículo: 'Auto', 'Moto', 'Bicicleta'
 * - cada almacén por separado: 'ALM:<código>' (central, norte, este)
 * "ocultos" vacío = se muestra todo.
 */
export type CapaMapa = string;

export type SemaforoUT = 'TODOS' | 'VACIO' | 'VERDE' | 'AMBAR' | 'ROJO';

export interface FiltrosPanel {
  ocultos: CapaMapa[];
  semaforoUT?: SemaforoUT;
}

export const FILTROS_VACIOS: FiltrosPanel = { ocultos: [], semaforoUT: 'TODOS' };

/** Capa de un almacén concreto */
export const capaAlmacen = (a: Almacen): CapaMapa => `ALM:${a.codigo}`;

/** Clasifica la ocupación de una unidad de transporte en niveles de semáforo + vacío */
export const nivelSemaforoUT = (u: UnidadTransporte): 'VACIO' | 'VERDE' | 'AMBAR' | 'ROJO' => {
  const cap = u.capacidadMaxima || 1;
  const pct = (u.cargaActual / cap) * 100;
  if (u.cargaActual <= 0) return 'VACIO';
  if (pct <= 50) return 'VERDE';
  if (pct <= 85) return 'AMBAR';
  return 'ROJO';
};

/** Clasifica la ocupación de un almacén en niveles de semáforo + vacío */
export const nivelSemaforoAlmacen = (a: Almacen): 'VACIO' | 'VERDE' | 'AMBAR' | 'ROJO' => {
  if (a.tipo === 'CENTRAL') return 'VERDE'; // Almacén central tiene stock infinito
  const stock = a.stockActual ?? 0;
  const cap = a.capacidadMaxima || 1000;
  const pct = (stock / cap) * 100;
  if (stock <= 0) return 'VACIO';
  if (pct <= 50) return 'VERDE';
  if (pct <= 85) return 'AMBAR';
  return 'ROJO';
};

/** El vehículo se dibuja si su tipo no está oculto y coincide con el semáforo seleccionado */
export const pasaFiltroUnidad = (u: UnidadTransporte, f?: FiltrosPanel): boolean => {
  if (!f) return true;
  if (f.ocultos.includes(u.tipoNombre)) return false;
  if (f.semaforoUT && f.semaforoUT !== 'TODOS') {
    if (nivelSemaforoUT(u) !== f.semaforoUT) return false;
  }
  return true;
};

/** El almacén se dibuja si no está oculto */
export const pasaFiltroAlmacen = (a: Almacen, f?: FiltrosPanel): boolean =>
  !f || !f.ocultos.includes(capaAlmacen(a));

/** Muestra u oculta una capa */
export const alternarCapa = (f: FiltrosPanel, capa: CapaMapa): FiltrosPanel => ({
  ...f,
  ocultos: f.ocultos.includes(capa) ? f.ocultos.filter((c) => c !== capa) : [...f.ocultos, capa],
});

/** Muestra u oculta un grupo completo (todos los vehículos o todos los almacenes) */
export const alternarGrupo = (f: FiltrosPanel, capas: CapaMapa[], mostrar: boolean): FiltrosPanel => ({
  ...f,
  ocultos: mostrar ? f.ocultos.filter((c) => !capas.includes(c)) : Array.from(new Set([...f.ocultos, ...capas])),
});