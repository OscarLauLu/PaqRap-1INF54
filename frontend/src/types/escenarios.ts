export type Escenario = 'DIA_A_DIA' | 'SIMULACION_5D' | 'COLAPSO_LOGISTICO';

export const ESCENARIOS: { id: Escenario; titulo: string; descripcion: string }[] = [
  { id: 'DIA_A_DIA', titulo: 'Operación día a día', descripcion: 'Simulación de la operación en tiempo real de las entregas.' },
  { id: 'SIMULACION_5D', titulo: 'Simulación 5D', descripcion: 'Simulación de 5 días de operación lógistica.' },
  { id: 'COLAPSO_LOGISTICO', titulo: 'Simulación de colapso', descripcion: 'Simulación de la operación hasta que se produzca un colapso.' },
];

export const NOMBRE_ESCENARIO: Record<Escenario, string> = {
  DIA_A_DIA: 'Operación día a día',
  SIMULACION_5D: 'Simulación 5D',
  COLAPSO_LOGISTICO: 'Simulación de colapso',
};

export const esEscenario = (v: unknown): v is Escenario =>
  typeof v === 'string' && ESCENARIOS.some((e) => e.id === v);
