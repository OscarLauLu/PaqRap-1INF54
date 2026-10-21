import React from 'react';
import { X, Wrench } from 'lucide-react';
import { Bodega, PinPedido, VehiculoRealista } from './IconosMapa';

interface LeyendaMapaProps {
  onClose?: () => void;
  /** Texto opcional junto al título, p. ej. "Simulación 5D" */
  escenario?: string;
}

// Mismos colores que usa MapaOperaciones: vibrantes y distintos para cada elemento
const COLOR = {
  central: '#EA580C',
  norte: '#0891B2',
  este: '#4F46E5',
  verde: '#22B957',
  ambar: '#F5B900',
  rojo: '#E53935',
  auto: '#2563EB',
  moto: '#9333EA',
  bici: '#EC4899',
  averia: '#FACC15',
};

const Seccion: React.FC<{ titulo: string; children: React.ReactNode }> = ({ titulo, children }) => (
  <div>
    <div className="rounded-md bg-(--color-brand-50) px-2 py-1 text-[11px] font-bold uppercase tracking-wide text-(--color-brand-700) mb-2">
      {titulo}
    </div>
    <div className="grid grid-cols-2 gap-x-3 gap-y-2 px-1">{children}</div>
  </div>
);

/** Ícono + nombre. La descripción aparece al pasar el mouse para no ocupar espacio. */
const Item: React.FC<{ icono: React.ReactNode; nombre: string; detalle: string }> = ({ icono, nombre, detalle }) => (
  <div className="flex items-center gap-2 min-w-0" title={detalle}>
    <div className="w-7 h-7 shrink-0 flex items-center justify-center">{icono}</div>
    <span className="text-[13px] font-medium text-(--color-ink-700) truncate">{nombre}</span>
  </div>
);

// Los íconos de la leyenda son los mismos componentes que dibuja el mapa, todos del mismo tamaño
const Pin: React.FC<{ color: string }> = ({ color }) => (
  <svg viewBox="-16 -35 32 37" className="w-6 h-6" aria-hidden="true">
    <PinPedido color={color} />
  </svg>
);
const Almacen: React.FC<{ color: string; letra: string }> = ({ color, letra }) => (
  <svg viewBox="-19 -19 46 46" className="w-6 h-6" aria-hidden="true">
    <Bodega color={color} letra={letra} />
  </svg>
);
const Vehiculo: React.FC<{ tipo: string; color: string }> = ({ tipo, color }) => (
  <svg viewBox="-20 -20 40 40" className="w-6 h-6" aria-hidden="true">
    <VehiculoRealista tipo={tipo} color={color} />
  </svg>
);

export const LeyendaMapa: React.FC<LeyendaMapaProps> = ({ onClose, escenario }) => {
  return (
    <div className="bg-white/95 backdrop-blur-xs border border-(--color-line-200) rounded-xl shadow-lg text-sm w-80 select-none animate-in fade-in zoom-in duration-150 overflow-hidden">
      {/* Encabezado */}
      <div className="flex items-center justify-between bg-(--color-brand-500) text-white px-3.5 py-2">
        <h3 className="font-bold text-sm truncate">
          Leyenda del mapa{escenario ? ` – ${escenario}` : ''}
        </h3>
        {onClose && (
          <button
            onClick={onClose}
            aria-label="Cerrar leyenda"
            className="text-white/80 hover:text-white p-0.5 rounded-sm hover:bg-white/10 shrink-0"
          >
            <X className="w-4 h-4" />
          </button>
        )}
      </div>

      <div className="p-3 space-y-3">
        <Seccion titulo="Pedidos (según criticidad)">
          <Item icono={<Pin color={COLOR.verde} />} nombre="A tiempo" detalle="Pedido con holgura suficiente (semáforo verde)" />
          <Item icono={<Pin color={COLOR.ambar} />} nombre="En riesgo" detalle="Pedido con poca holgura (semáforo ámbar)" />
          <Item icono={<Pin color={COLOR.rojo} />} nombre="Crítico" detalle="Pedido por vencer o vencido (semáforo rojo)" />
        </Seccion>

        <Seccion titulo="Almacenes">
          <Item icono={<Almacen color={COLOR.central} letra="C" />} nombre="Central" detalle="Almacén Central (C): punto principal de abastecimiento" />
          <Item icono={<Almacen color={COLOR.norte} letra="N" />} nombre="Norte" detalle="Almacén Norte (N): almacén intermedio" />
          <Item icono={<Almacen color={COLOR.este} letra="E" />} nombre="Este" detalle="Almacén Este (E): almacén intermedio" />
        </Seccion>

        <Seccion titulo="Vehículos (vistos desde arriba)">
          <Item icono={<Vehiculo tipo="Auto" color={COLOR.auto} />} nombre="Auto" detalle="El frente apunta hacia donde avanza (ej. TA01)" />
          <Item icono={<Vehiculo tipo="Moto" color={COLOR.moto} />} nombre="Moto" detalle="El frente apunta hacia donde avanza (ej. TM01)" />
          <Item icono={<Vehiculo tipo="Bicicleta" color={COLOR.bici} />} nombre="Bicicleta" detalle="El frente apunta hacia donde avanza (ej. TB01)" />
          <Item
            icono={
              <span className="w-5 h-5 rounded-full border border-[#111827] flex items-center justify-center shadow-sm" style={{ backgroundColor: COLOR.averia }}>
                <Wrench className="w-3 h-3 text-[#111827]" strokeWidth={3} />
              </span>
            }
            nombre="Avería"
            detalle="Cualquier vehículo averiado (auto, moto o bicicleta) se ve atenuado y con esta señal amarilla y negra encima"
          />
        </Seccion>

        <Seccion titulo="Incidencias y rutas">
          <Item
            icono={
              <svg viewBox="0 0 28 14" className="w-6 h-4" aria-hidden="true">
                <line x1="1" y1="7" x2="27" y2="7" stroke="#111827" strokeWidth="2.5" strokeLinecap="round" />
                <rect x="8" y="2.5" width="12" height="9" rx="1.5" fill="white" stroke="#111827" strokeWidth="1" />
                <path d="M10 11 L13 3 M15 11 L18 3" stroke="#DC2626" strokeWidth="1.8" />
              </svg>
            }
            nombre="Bloqueo"
            detalle="Bloqueo en vía: tramo no transitable"
          />
          <Item
            icono={<div className="w-6 border-t-2 border-dashed border-[#6B7280]" />}
            nombre="Ruta planificada"
            detalle="Camino que recorre el vehículo, calle por calle"
          />
        </Seccion>
      </div>
    </div>
  );
};