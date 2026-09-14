export interface Ubicacion {
  x: number;
  y: number;
}

export interface Almacen {
  id: number;
  codigo: string;
  nombre: string;
  tipo: 'CENTRAL' | 'INTERMEDIO';
  ubicacion: Ubicacion;
  stockActual: number;
  capacidadMaxima?: number | null;
  porcentajeOcupacion: number;
  superaAlertaOcupacion: boolean;
}

export type TipoVehiculo = 'Auto' | 'Moto' | 'Bicicleta' | string;
export type EstadoOperativo = 'DISPONIBLE' | 'EN_RUTA' | 'EN_MANTENIMIENTO' | 'AVERIADO';

export interface UnidadTransporte {
  id: number;
  codigo: string;
  tipoNombre: TipoVehiculo;
  capacidadMaxima: number;
  cargaActual: number;
  capacidadDisponible: number;
  porcentajeCarga: number;
  velocidadPromedioKmH: number;
  costoPorKm: number;
  estadoOperativo: EstadoOperativo;
  colorEstado: string;
  ubicacionActual: Ubicacion;
  nombreConductor?: string;
  turno?: string;
  pedidoAsignado?: string;
  activo: boolean;
}

export interface Pedido {
  id: number;
  codigo: string;
  cliente: string;
  cantidad: number;
  destino: Ubicacion;
  estado: string;
  criticidad: 'VERDE' | 'AMBAR' | 'ROJO';
  colorCriticidad: string;
  plazoLimite?: string;
}

export interface BloqueoVial {
  id: number;
  codigo: string;
  fechaHoraInicio: string;
  fechaHoraFin: string;
  activo: boolean;
  coordenadasNodos: string;
  ubicacionTexto?: string;
  impacto?: string;
}

export interface AveriaVial {
  id: number;
  codigo: string;
  tipo: 'TIPO_1' | 'TIPO_2' | 'TIPO_3';
  fechaHoraEvento: string;
  ubicacionFalla: Ubicacion;
  horaReincorporacion?: string;
  resuelta: boolean;
  unidadCodigo?: string;
  impacto?: string;
}

export interface IncidenteItem {
  id: string | number;
  numero: number;
  tipo: 'averia' | 'bloqueo';
  evento: string;
  ubicacion: string;
  impacto: string;
  dia: number;
  hora: string;
  estado: 'Resuelta' | 'En curso' | 'Pendiente';
}

export interface RelojSimuladoData {
  relojFormateado: string;
  diaSimulado: number;
  horaSimulada: string;
  instanteActual: string;
  factorAceleracion: number;
  estadoEjecucion: 'CONFIGURADA' | 'EN_EJECUCION' | 'DETENIDA' | 'FINALIZADA' | 'COLAPSO_LOGISTICO';
  pedidosEnCurso?: number;
  totalDias?: number;
}

export interface MapaOperacionesData {
  relojSimulado: string;
  instanteActual: string;
  unidades: Array<{
    id: number;
    codigo: string;
    tipo: string;
    estadoOperativo: string;
    colorEstado: string;
    ubicacion: Ubicacion;
    cargaActual: number;
    capacidadMaxima: number;
  }>;
  pedidos: Pedido[];
  bloqueos: BloqueoVial[];
  averias: AveriaVial[];
}

export interface AlertaItem {
  id: number;
  codigo: string;
  tipo: string;
  mensaje: string;
  fechaHoraGeneracion: string;
  atendida: boolean;
}
