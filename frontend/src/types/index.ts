export interface Ubicacion {
  posX: number;
  posY: number;
}

// Coincide con AlmacenDTO.java: la clave es `codigo` (String), no un `id` autoincremental
// (remodelado a clave natural, ver CLAUDE.md). stockActual es null para el Almacén Central.
export interface Almacen {
  codigo: string;
  nombre: string;
  tipo: 'CENTRAL' | 'INTERMEDIO';
  ubicacion: Ubicacion;
  stockActual: number | null;
  capacidadMaxima?: number | null;
  porcentajeOcupacion: number;
  superaAlertaOcupacion: boolean;
}

export type TipoVehiculo = 'Auto' | 'Moto' | 'Bicicleta' | string;
export type EstadoOperativo = 'DISPONIBLE' | 'EN_RUTA' | 'EN_MANTENIMIENTO' | 'AVERIADO';

// Coincide con UnidadTransporteDTO.java: la clave es `codigo` (String); el DTO no expone
// `turno` ni `pedidoAsignado` (eso vive en asignacion_turno / Ruta, se agrega en otro punto).
export interface UnidadTransporte {
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

// Coincide exactamente con EstadoPedido.java (RF-33): 5 valores, sin "Reasignado".
export type EstadoPedido = 'REGISTRADO' | 'PLANIFICADO' | 'EN_RUTA' | 'ENTREGADO' | 'CANCELADO';

export const ESTADO_PEDIDO_LABEL: Record<EstadoPedido, string> = {
  REGISTRADO: 'Por atender',
  PLANIFICADO: 'Planificado',
  EN_RUTA: 'En ruta',
  ENTREGADO: 'Entregado',
  CANCELADO: 'Cancelado',
};

export interface Pedido {
  id: number;
  codigo: string;
  cliente: string;
  cantidad: number;
  destino: Ubicacion;
  estado: EstadoPedido;
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
  archivoOrigen?: string;
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

export type EstadoEjecucionSimulacion =
  | 'CONFIGURADA'
  | 'EN_EJECUCION'
  | 'DETENIDA'
  | 'FINALIZADA'
  | 'DETENIDA_POR_COLAPSO';

export interface RelojSimuladoData {
  relojFormateado: string;
  diaSimulado: number;
  horaSimulada: string;
  instanteActual: string;
  factorAceleracion: number;
  estadoEjecucion: EstadoEjecucionSimulacion;
  pedidosEnCurso?: number;
  totalDias?: number;
}

export interface MapaOperacionesData {
  relojSimulado: string;
  instanteActual: string;
  unidades: Array<{
    id: string;
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

// Coincide con MotorSimulacion.consultarMetricas() (/api/simulacion/metricas).
export interface MetricasSimulacion {
  totalPedidosIngresados: number;
  pedidosEnPlazo: number;
  pedidosTarde: number;
  costoAcumuladoTotal: number;
  costoAuto: number;
  costoMoto: number;
  costoBici: number;
  entregasAuto: number;
  entregasMoto: number;
  entregasBici: number;
  bloqueosOcurridos: number;
  averiasOcurridas: number;
  instanteColapso: string | null;
  volumenPedidosColapso: number | null;
}

// Coincide con ResultadoSimulacion.java (RF-73 a RF-75).
export interface ResultadoSimulacion {
  id: number;
  codigo: string;
  escenario: string;
  fechaHoraInicio: string;
  fechaHoraFin: string;
  tiempoEjecucionRealSegundos: number;
  instanteColapso: string | null;
  volumenPedidosColapso: number | null;
  totalPedidos: number;
  pedidosEntregadosEnPlazo: number;
  pedidosEntregadosTarde: number;
  porcentajeCumplimiento: number;
  costoTotalOperacion: number;
  costoTotalAuto: number;
  costoTotalMoto: number;
  costoTotalBicicleta: number;
  entregasAuto: number;
  entregasMoto: number;
  entregasBicicleta: number;
  totalBloqueosOcurridos: number;
  totalAveriasOcurridas: number;
}

export type TipoEscenario = 'DIA_A_DIA' | 'SIMULACION_5D' | 'COLAPSO_LOGISTICO';

// Coincide con ConfiguracionSemaforo.java: los valores por defecto reales del backend
// (Verde >= 12h, Ámbar >= 4h) — no los valores referenciales que traían las pantallas de Figma.
export interface SemaforoCriticidad {
  horasVerde: number;
  horasAmbar: number;
}

// Coincide con ParametrosSimulacion.java (RF-64).
export interface ParametrosSimulacionRequest {
  numAutos: number;
  numMotos: number;
  numBicicletas: number;
  capacidadAlmacenIntermedio?: number;
  tasaIncrementoPedidos?: number;
  archivoPedidos?: string;
  archivoBloqueos?: string;
}
