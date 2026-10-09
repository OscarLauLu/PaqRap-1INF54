import React from 'react';
import { X, Car, Bike, Wrench } from 'lucide-react';

interface LeyendaMapaProps {
  onClose?: () => void;
  /** Texto opcional junto al título, p. ej. "Simulación 5D" */
  escenario?: string;
}

// Mismos colores que usa MapaOperaciones para dibujar cada elemento
const COLOR = {
  central: '#DC2626',
  norte: '#F97316',
  este: '#9333EA',
  cliente: '#2563EB',
  auto: '#3B82F6',
  moto: '#16A34A',
  bici: '#EAB308',
  averia: '#374151',
  bloqueo: '#DC2626',
};

const Seccion: React.FC<{ titulo: string; children: React.ReactNode }> = ({ titulo, children }) => (
  <div className="rounded-lg border border-(--color-brand-100) overflow-hidden">
    <div className="bg-(--color-brand-50) px-3 py-1.5 text-[11px] font-bold uppercase tracking-wide text-(--color-brand-700)">
      {titulo}
    </div>
    <div className="px-3 py-2 space-y-2.5">{children}</div>
  </div>
);

const Item: React.FC<{ icono: React.ReactNode; nombre: string; detalle: string }> = ({ icono, nombre, detalle }) => (
  <div className="flex items-center gap-3">
    <div className="w-8 h-8 shrink-0 flex items-center justify-center">{icono}</div>
    <div className="leading-tight">
      <div className="font-semibold text-(--color-ink-900) text-xs">{nombre}</div>
      <div className="text-[11px] text-(--color-ink-400)">{detalle}</div>
    </div>
  </div>
);

/** Cuadrado con casita y letra, igual al marcador de almacén del mapa */
const IconoAlmacen: React.FC<{ color: string; letra: string }> = ({ color, letra }) => (
  <svg viewBox="-1.5 -1.5 3 3" className="w-7 h-7" aria-hidden="true">
    <rect x="-1.5" y="-1.5" width="3" height="3" rx="0.5" fill={color} />
    <path d="M -1 -0.2 L 0 -1.2 L 1 -0.2" stroke="white" strokeWidth="0.2" fill="none" />
    <text x="0" y="1" textAnchor="middle" fill="white" fontSize="1.5" fontWeight="bold">{letra}</text>
  </svg>
);

const IconoVehiculo: React.FC<{ color: string; tipo: 'auto' | 'bici' }> = ({ color, tipo }) =>
  tipo === 'auto' ? (
    <Car className="w-7 h-7" stroke={color} strokeWidth={2.2} aria-hidden="true" />
  ) : (
    <Bike className="w-7 h-7" stroke={color} strokeWidth={2.2} aria-hidden="true" />
  );

export const LeyendaMapa: React.FC<LeyendaMapaProps> = ({ onClose, escenario }) => {
  return (
    <div className="bg-white/95 backdrop-blur-xs border border-(--color-line-200) rounded-xl shadow-xl text-xs w-72 max-h-[70vh] flex flex-col select-none animate-in fade-in zoom-in duration-150 overflow-hidden">
      {/* Encabezado */}
      <div className="flex items-center justify-between bg-(--color-brand-500) text-white px-4 py-2.5 shrink-0">
        <h3 className="font-bold text-sm">
          Leyenda del mapa{escenario ? ` – ${escenario}` : ''}
        </h3>
        {onClose && (
          <button
            onClick={onClose}
            aria-label="Cerrar leyenda"
            className="text-white/80 hover:text-white p-0.5 rounded-sm hover:bg-white/10"
          >
            <X className="w-4 h-4" />
          </button>
        )}
      </div>

      <div className="p-3 space-y-3 overflow-y-auto">
        <Seccion titulo="Almacenes">
          <Item icono={<IconoAlmacen color={COLOR.central} letra="C" />} nombre="Almacén Central (C)" detalle="Punto principal de abastecimiento" />
          <Item icono={<IconoAlmacen color={COLOR.norte} letra="N" />} nombre="Almacén Norte (N)" detalle="Almacén intermedio" />
          <Item icono={<IconoAlmacen color={COLOR.este} letra="E" />} nombre="Almacén Este (E)" detalle="Almacén intermedio" />
        </Seccion>

        <Seccion titulo="Clientes (pedidos)">
          <Item
            icono={<span className="w-5 h-5 rounded-full" style={{ backgroundColor: COLOR.cliente }} />}
            nombre="Cliente (pedido)"
            detalle="Muestra el código del pedido en el mapa"
          />
        </Seccion>

        <Seccion titulo="Vehículos">
          <Item icono={<IconoVehiculo color={COLOR.auto} tipo="auto" />} nombre="Auto" detalle="Vehículo para reparto (ej. A3)" />
          <Item icono={<IconoVehiculo color={COLOR.moto} tipo="bici" />} nombre="Moto" detalle="Vehículo para reparto (ej. M1)" />
          <Item icono={<IconoVehiculo color={COLOR.bici} tipo="bici" />} nombre="Bicicleta" detalle="Vehículo para reparto (ej. B2)" />
        </Seccion>

        <Seccion titulo="Incidencias">
          <Item
            icono={
              <span className="w-7 h-7 rounded-full flex items-center justify-center" style={{ backgroundColor: COLOR.averia }}>
                <Wrench className="w-4 h-4 text-white" aria-hidden="true" />
              </span>
            }
            nombre="Avería (vehículo)"
            detalle="Indica un vehículo averiado en el mapa"
          />
          <Item
            icono={
              <svg viewBox="0 0 28 14" className="w-8 h-4" aria-hidden="true">
                <line x1="1" y1="7" x2="27" y2="7" stroke="#111827" strokeWidth="2.5" strokeLinecap="round" />
                <rect x="8" y="3" width="12" height="8" rx="1.5" fill={COLOR.bloqueo} />
              </svg>
            }
            nombre="Bloqueo en vía"
            detalle="Tramo de la vía no transitable"
          />
        </Seccion>

        <Seccion titulo="Rutas">
          <Item
            icono={<div className="w-7 border-t-2 border-dashed border-(--color-ink-500)" />}
            nombre="Ruta planificada"
            detalle="Recorrido asignado al vehículo"
          />
        </Seccion>
      </div>
    </div>
  );
};