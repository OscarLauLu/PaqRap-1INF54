import React from 'react';

export type Tono = 'verde' | 'ambar' | 'rojo' | 'azul' | 'gris';

const ESTILOS: Record<Tono, string> = {
  verde: 'bg-green-50 text-green-700 ring-green-200',
  ambar: 'bg-amber-50 text-amber-700 ring-amber-200',
  rojo: 'bg-red-50 text-red-700 ring-red-200',
  azul: 'bg-blue-50 text-blue-700 ring-blue-200',
  gris: 'bg-gray-100 text-gray-600 ring-gray-200',
};

export const Badge: React.FC<{ tono: Tono; children: React.ReactNode }> = ({ tono, children }) => (
  <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 whitespace-nowrap ${ESTILOS[tono]}`}>
    {children}
  </span>
);

const TONO_ESTADO_PEDIDO: Record<string, Tono> = {
  REGISTRADO: 'ambar',
  PLANIFICADO: 'azul',
  EN_RUTA: 'azul',
  ENTREGADO: 'verde',
  CANCELADO: 'gris',
};

const LABEL_ESTADO_PEDIDO: Record<string, string> = {
  REGISTRADO: 'Por atender',
  PLANIFICADO: 'Planificado',
  EN_RUTA: 'En ruta',
  ENTREGADO: 'Entregado',
  CANCELADO: 'Cancelado',
};

/** Traduce el EstadoPedido real del backend (REGISTRADO, PLANIFICADO, EN_RUTA, ENTREGADO,
 * CANCELADO) a la etiqueta en español usada en las pantallas, sin inventar estados nuevos. */
export const BadgeEstadoPedido: React.FC<{ estado: string }> = ({ estado }) => (
  <Badge tono={TONO_ESTADO_PEDIDO[estado] || 'gris'}>{LABEL_ESTADO_PEDIDO[estado] || estado}</Badge>
);

const TONO_CRITICIDAD: Record<string, Tono> = { VERDE: 'verde', AMBAR: 'ambar', ROJO: 'rojo' };

export const BadgeCriticidad: React.FC<{ criticidad: string }> = ({ criticidad }) => (
  <Badge tono={TONO_CRITICIDAD[criticidad] || 'gris'}>{criticidad}</Badge>
);
