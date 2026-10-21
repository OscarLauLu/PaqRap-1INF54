import React from 'react';

/**
 * Íconos del mapa de operaciones. Cada uno se dibuja en su propia escala (en "unidades de diseño")
 * y quien lo usa lo escala al tamaño del mapa. Cada tipo de elemento tiene una forma distinta,
 * para que no se confundan entre sí:
 * - Pedidos: PIN (es la forma universal de "destino"), con el color de su criticidad.
 * - Vehículos: vistos DESDE ARRIBA, lo único que se mueve y gira.
 * - Almacenes: lo único CUADRADO, con una bodega de puerta enrollable.
 */

// ===================== VEHÍCULOS (vistos desde arriba, mirando a la derecha = 0°) =====================
// Miden unos 36 de largo. Al rotarlos, el frente del dibujo es el frente real del vehículo.

const Auto: React.FC<{ color: string }> = ({ color }) => (
  <>
    {/* Carrocería */}
    <rect x="-18" y="-10" width="36" height="20" rx="7" fill={color} stroke="#0F172A" strokeOpacity="0.25" strokeWidth="1" />
    {/* Parabrisas (adelante) y luneta (atrás): indican cuál es el frente */}
    <path d="M3 -8 L10 -6.5 V6.5 L3 8 Z" fill="#BFDBFE" />
    <rect x="-14" y="-7" width="5" height="14" rx="2" fill="#BFDBFE" opacity="0.9" />
    {/* Techo */}
    <rect x="-8" y="-8" width="10" height="16" rx="2.5" fill={color} stroke="white" strokeOpacity="0.35" />
    {/* Faros (amarillos) y luces traseras (rojas) */}
    <rect x="15.5" y="-8" width="2.2" height="3.6" rx="1" fill="#FEF08A" />
    <rect x="15.5" y="4.4" width="2.2" height="3.6" rx="1" fill="#FEF08A" />
    <rect x="-18" y="-8" width="1.6" height="3.4" fill="#EF4444" />
    <rect x="-18" y="4.6" width="1.6" height="3.4" fill="#EF4444" />
    {/* Espejos */}
    <rect x="4" y="-12.5" width="3.4" height="2.5" rx="1" fill={color} />
    <rect x="4" y="10" width="3.4" height="2.5" rx="1" fill={color} />
  </>
);

const Moto: React.FC<{ color: string }> = ({ color }) => (
  <>
    {/* Ruedas gruesas (más anchas que las de la bicicleta) */}
    <rect x="-18" y="-4.2" width="10" height="8.4" rx="4.2" fill="#111827" />
    <rect x="8" y="-4.2" width="10" height="8.4" rx="4.2" fill="#111827" />
    {/* Cuerpo ancho y manubrio */}
    <path d="M-12 -6.5 Q-1 -10 10 -5.5 L10 5.5 Q-1 10 -12 6.5 Z" fill={color} stroke="white" strokeWidth="1.2" />
    <path d="M7 -12.5 Q9.5 0 7 12.5" fill="none" stroke="#111827" strokeWidth="3" strokeLinecap="round" />
    {/* Conductor con casco y visor (mirando hacia adelante) */}
    <ellipse cx="-3" cy="0" rx="6.5" ry="8.5" fill="#1E293B" />
    <circle cx="-2" cy="0" r="5.2" fill={color} stroke="white" strokeWidth="1.4" />
    <rect x="1" y="-2.6" width="2.8" height="5.2" rx="1.2" fill="#0F172A" />
  </>
);

const Bicicleta: React.FC<{ color: string }> = ({ color }) => (
  <>
    {/* Ruedas (más delgadas que las de la moto, pero bien visibles) y cuadro */}
    <rect x="-19" y="-2.4" width="12" height="4.8" rx="2.4" fill="#111827" />
    <rect x="7" y="-2.4" width="12" height="4.8" rx="2.4" fill="#111827" />
    <line x1="-11" y1="0" x2="9" y2="0" stroke={color} strokeWidth="4" strokeLinecap="round" />
    <path d="M8 -10.5 Q10 0 8 10.5" fill="none" stroke="#111827" strokeWidth="2.8" strokeLinecap="round" />
    {/* Brazos del ciclista hacia el manubrio, cuerpo y casco */}
    <path d="M-6 -7.5 Q2 -9.5 7 -10 M-6 7.5 Q2 9.5 7 10" fill="none" stroke="#F1C9A5" strokeWidth="3" strokeLinecap="round" />
    <ellipse cx="-4" cy="0" rx="6" ry="9" fill={color} stroke="white" strokeWidth="1.2" />
    <circle cx="-2.5" cy="0" r="4.6" fill="#FBBF24" stroke="white" strokeWidth="1.2" />
  </>
);

export const VehiculoRealista: React.FC<{ tipo: string; color: string }> = ({ tipo, color }) => {
  if (tipo === 'Auto') return <Auto color={color} />;
  if (tipo === 'Moto') return <Moto color={color} />;
  return <Bicicleta color={color} />;
};

// ===================== PEDIDOS (pin de ubicación con una caja) =====================
// La punta del pin está en (0, 0): es el punto exacto de entrega. Mide unos 32 de alto.

export const PinPedido: React.FC<{ color: string }> = ({ color }) => (
  <>
    <path
      d="M0 0 C-10 -10 -11 -15 -11 -20 A11 11 0 1 1 11 -20 C11 -15 10 -10 0 0 Z"
      fill={color}
      stroke="white"
      strokeWidth="2"
    />
    {/* Caja (paquete) en blanco dentro del pin */}
    <g transform="translate(0 -20) scale(0.95)">
      <path d="M-6 -3 L0 -6 L6 -3 L6 4 L0 7 L-6 4 Z" fill="white" />
      <path d="M-6 -3 L0 0 L6 -3 M0 0 V7" fill="none" stroke={color} strokeWidth="1.2" />
    </g>
  </>
);

// ===================== ALMACENES (bodega con puerta enrollable + etiqueta con la letra) =====================
// Centrado en (0, 0). Mide 34 × 34; la etiqueta de la letra va en la esquina inferior derecha.

export const Bodega: React.FC<{ color: string; letra: string }> = ({ color, letra }) => (
  <>
    <rect x="-17" y="-17" width="34" height="34" rx="8" fill={color} stroke="white" strokeWidth="2.6" />
    {/* Techo a dos aguas, paredes y puerta enrollable */}
    <g transform="translate(-12.5 -13.5) scale(1.05)">
      <path d="M3.5 10.5 L12 4.5 L20.5 10.5" fill="none" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M5.5 10 V19.5 H18.5 V10" fill="none" stroke="white" strokeWidth="1.8" strokeLinejoin="round" />
      <rect x="8.5" y="12.5" width="7" height="7" rx="0.4" fill="white" />
      <line x1="8.5" y1="14.8" x2="15.5" y2="14.8" stroke={color} strokeWidth="0.8" />
      <line x1="8.5" y1="17.1" x2="15.5" y2="17.1" stroke={color} strokeWidth="0.8" />
    </g>
    {/* Etiqueta con la letra del almacén (C, N, E) */}
    <circle cx="16" cy="16" r="8" fill="white" stroke={color} strokeWidth="2" />
    <text x="16" y="19.5" textAnchor="middle" fontSize="10" fontWeight="bold" fill={color}>
      {letra}
    </text>
  </>
);