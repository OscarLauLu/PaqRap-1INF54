import type { Ubicacion } from './index';

// TODO: alinear con TipoEntrega.java del backend cuando se conecte
export const TIPOS_ENTREGA = ['Normal', 'Priorizada'] as const;
export type TipoEntregaRegistro = (typeof TIPOS_ENTREGA)[number];

// TODO: confirmar con el grupo qué plazos se permiten
export const PLAZOS_HORAS = [8, 12, 36] as const;

export type EstadoPedidoRegistro = 'Por atender' | 'Planificado';

export interface PedidoRegistro {
  id: string;
  idCliente: string;
  cliente: string;
  ubicacion: Ubicacion;
  cantidad: number;
  fechaRegistro: string; // ISO
  plazoHoras: number;
  tipoEntrega: TipoEntregaRegistro;
  estado: EstadoPedidoRegistro;
}

export interface NuevoPedidoDatos {
  cliente: string;
  ubicacion: Ubicacion;
  cantidad: number;
  tipoEntrega: TipoEntregaRegistro;
  plazoHoras: number;
}

// ----- Averías -----
export type TipoAveria = 'TIPO_1' | 'TIPO_2' | 'TIPO_3';

// TODO: confirmar con el grupo los estados posibles
export type EstadoAveria = 'En reparación' | 'Resuelta';

export interface NuevaAveriaDatos {
  idUnidad: string;
  ubicacion: Ubicacion;
  tipo: TipoAveria;
  inicio: string; // ISO
  fin: string; // ISO (fin de indisponibilidad)
  estado: EstadoAveria;
}

export interface AveriaRegistrada extends NuevaAveriaDatos {
  id: string;
}
