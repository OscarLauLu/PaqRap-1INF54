import React from 'react';

/**
 * Íconos de vehículos vistos desde arriba (vista cenital), dibujados mirando a la DERECHA (0°).
 * Así, al rotarlos, el frente del vehículo siempre apunta hacia donde avanza, como en un GPS.
 * Están dibujados en una caja de -12 a 12; se escalan al usarlos dentro de la insignia.
 */

const Auto: React.FC<{ color: string }> = ({ color }) => (
  <>
    <rect x="-11" y="-6.5" width="22" height="13" rx="4.5" fill="white" />
    {/* Parabrisas adelante y luneta atrás, para que se note cuál es el frente */}
    <path d="M2.5 -5 L6.5 -4 V4 L2.5 5 Z" fill={color} />
    <rect x="-8.5" y="-4" width="3" height="8" rx="1.2" fill={color} opacity="0.7" />
    <rect x="0.5" y="-8.5" width="2.4" height="2" rx="0.8" fill="white" />
    <rect x="0.5" y="6.5" width="2.4" height="2" rx="0.8" fill="white" />
  </>
);

const Moto: React.FC<{ color: string }> = ({ color }) => (
  <>
    {/* Ruedas gruesas y cuerpo ancho: se diferencia de la bicicleta */}
    <rect x="-11.5" y="-1.8" width="6" height="3.6" rx="1.8" fill="white" />
    <rect x="5.5" y="-1.8" width="6.5" height="3.6" rx="1.8" fill="white" />
    <ellipse cx="-0.5" cy="0" rx="7" ry="4" fill="white" />
    <path d="M5 -7 Q6.4 0 5 7" fill="none" stroke="white" strokeWidth="2.2" strokeLinecap="round" />
    {/* Casco del conductor */}
    <circle cx="-1.5" cy="0" r="3" fill={color} stroke="white" strokeWidth="1" />
  </>
);

const Bicicleta: React.FC<{ color: string }> = ({ color }) => (
  <>
    {/* Ruedas delgadas y cuadro fino: se ve más liviana que la moto */}
    <rect x="-12" y="-1" width="8" height="2" rx="1" fill="white" />
    <rect x="4" y="-1" width="8" height="2" rx="1" fill="white" />
    <line x1="-8" y1="0" x2="6" y2="0" stroke="white" strokeWidth="1.4" />
    <path d="M5 -6 Q6 0 5 6" fill="none" stroke="white" strokeWidth="1.8" strokeLinecap="round" />
    {/* Hombros y cabeza del ciclista */}
    <ellipse cx="-1.5" cy="0" rx="2.4" ry="4.2" fill="white" />
    <circle cx="-0.8" cy="0" r="2" fill={color} stroke="white" strokeWidth="0.9" />
  </>
);

export const GlifoVehiculo: React.FC<{ tipo: string; color: string }> = ({ tipo, color }) => {
  if (tipo === 'Auto') return <Auto color={color} />;
  if (tipo === 'Moto') return <Moto color={color} />;
  return <Bicicleta color={color} />;
};