import type { EstadoPedido, Ubicacion } from './index';

// Coincide con TipoEntrega.java (RF-28): 36h es la única opción Regular; 4/8/12/18h son Priorizada.
export const TIPOS_ENTREGA = ['Regular', 'Priorizada'] as const;
export type TipoEntregaRegistro = (typeof TIPOS_ENTREGA)[number];

export const PLAZOS_POR_TIPO: Record<TipoEntregaRegistro, number[]> = {
  Regular: [36],
  Priorizada: [4, 8, 12, 18],
};

/** Nombre real del enum TipoEntrega.java a partir de lo elegido en el formulario. */
export const tipoEntregaEnum = (tipo: TipoEntregaRegistro, plazoHoras: number): string =>
  tipo === 'Regular' ? 'REGULAR_36H' : `PRIORIZADA_${plazoHoras}H`;

export interface NuevoPedidoDatos {
  cliente: string;
  ubicacion: Ubicacion;
  cantidad: number;
  tipoEntrega: TipoEntregaRegistro;
  plazoHoras: number;
}

/** Fila tal como la devuelve GET /api/pedidos (PedidoDTO), ya mapeada para la tabla de Registro. */
export interface PedidoRegistro {
  id: number;
  codigo: string;
  idCliente: string;
  cliente: string;
  ubicacion: Ubicacion;
  cantidad: number;
  fechaRegistro: string; // ISO, = fechaHoraRegistro
  plazoHoras: number;
  tipoEntregaEnum: string; // p.ej. PRIORIZADA_8H
  estado: EstadoPedido;
  plazoLimiteEntrega?: string;
}

// ----- Bloqueos -----

/** Registro manual de un único bloqueo (distinto de la carga masiva por archivo). */
export interface NuevoBloqueoDatos {
  inicio: string; // ISO
  fin: string; // ISO
  coordenadasNodos: string; // "x1,y1,x2,y2,..."
}

export type EstadoBloqueoDerivado = 'Programado' | 'Activo' | 'Vencido';

/** El backend no guarda un campo "estado": se deriva de fechaHoraInicio/fechaHoraFin vs. ahora. */
export const derivarEstadoBloqueo = (inicio: string, fin: string, ahora = new Date()): EstadoBloqueoDerivado => {
  const i = new Date(inicio);
  const f = new Date(fin);
  if (ahora < i) return 'Programado';
  if (ahora > f) return 'Vencido';
  return 'Activo';
};

// ----- Averías -----
export type TipoAveria = 'TIPO_1' | 'TIPO_2' | 'TIPO_3';

/** Datos que el usuario ingresa. La reincorporación y el estado los calcula el backend
 * (Averia.calcularReincorporacion, RF-14) — no se piden como campos libres. */
export interface NuevaAveriaDatos {
  idUnidad: string;
  ubicacion: Ubicacion;
  tipo: TipoAveria;
}

export interface AveriaRegistrada {
  id: number;
  codigo: string;
  idUnidad: string;
  ubicacion: Ubicacion;
  tipo: TipoAveria;
  fechaHoraEvento: string;
  horaReincorporacion: string | null;
  resuelta: boolean;
}
