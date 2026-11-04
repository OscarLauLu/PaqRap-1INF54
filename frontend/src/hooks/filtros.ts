import type { Almacen, UnidadTransporte } from '../types';

/**
 * Qué se muestra en el mapa. Desde el panel derecho se puede ocultar:
 * - cada tipo de vehículo: 'Auto', 'Moto', 'Bicicleta'
 * - cada almacén por separado: 'ALM:<código>' (central, norte, este)
 * "ocultos" vacío = se muestra todo.
 */
export type CapaMapa = string;

export interface FiltrosPanel {
  ocultos: CapaMapa[];
}

export const FILTROS_VACIOS: FiltrosPanel = { ocultos: [] };

/** Capa de un almacén concreto */
export const capaAlmacen = (a: Almacen): CapaMapa => `ALM:${a.codigo}`;

/** El vehículo se dibuja si su tipo no está oculto */
export const pasaFiltroUnidad = (u: UnidadTransporte, f?: FiltrosPanel): boolean =>
  !f || !f.ocultos.includes(u.tipoNombre);

/** El almacén se dibuja si no está oculto */
export const pasaFiltroAlmacen = (a: Almacen, f?: FiltrosPanel): boolean =>
  !f || !f.ocultos.includes(capaAlmacen(a));

/** Muestra u oculta una capa */
export const alternarCapa = (f: FiltrosPanel, capa: CapaMapa): FiltrosPanel => ({
  ocultos: f.ocultos.includes(capa) ? f.ocultos.filter((c) => c !== capa) : [...f.ocultos, capa],
});

/** Muestra u oculta un grupo completo (todos los vehículos o todos los almacenes) */
export const alternarGrupo = (f: FiltrosPanel, capas: CapaMapa[], mostrar: boolean): FiltrosPanel => ({
  ocultos: mostrar ? f.ocultos.filter((c) => !capas.includes(c)) : Array.from(new Set([...f.ocultos, ...capas])),
});