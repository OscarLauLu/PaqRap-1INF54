import React from 'react';
import { X, MousePointerClick } from 'lucide-react';
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
};

// Averías por tipo (mismos colores y números que el mapa)
// Escala de riesgo: amarillo (bajo) → naranja (medio) → rojo oscuro (alto)
const AVERIAS = [
  { numero: '1', nombre: 'Avería tipo 1', detalle: 'leve: 2 h en el lugar (riesgo bajo)', color: '#FDE047', texto: '#713F12' },
  { numero: '2', nombre: 'Avería tipo 2', detalle: 'moderada: hasta el cambio de turno (riesgo medio)', color: '#F97316', texto: '#FFFFFF' },
  { numero: '3', nombre: 'Avería tipo 3', detalle: 'grave: 2 días o más en taller (riesgo alto)', color: '#B91C1C', texto: '#FFFFFF' },
];

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
const Pin: React.FC<{ color: string; atenuado?: boolean }> = ({ color, atenuado }) => (
  <svg viewBox="-16 -35 32 37" className="w-6 h-6" aria-hidden="true" opacity={atenuado ? 0.5 : 1}>
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

/** Línea punteada de ruta, igual que en el mapa: suave normalmente y gruesa cuando el vehículo está seleccionado */
const LineaRuta: React.FC<{ color: string; seleccionada?: boolean }> = ({ color, seleccionada }) => (
  <svg viewBox="0 0 28 10" className="w-7 h-3" aria-hidden="true">
    <line
      x1="1"
      y1="5"
      x2="27"
      y2="5"
      stroke={color}
      strokeOpacity={seleccionada ? 1 : 0.6}
      strokeWidth={seleccionada ? 3 : 1.8}
      strokeDasharray="4 2.5"
      strokeLinecap="round"
    />
  </svg>
);

export const LeyendaMapa: React.FC<LeyendaMapaProps> = ({ onClose, escenario }) => {
  return (
    <div className="bg-white/95 backdrop-blur-xs border border-(--color-line-200) rounded-xl shadow-lg text-sm w-80 max-h-full flex flex-col select-none animate-in fade-in zoom-in duration-150 overflow-hidden">
      {/* Encabezado */}
      <div className="shrink-0 flex items-center justify-between bg-(--color-brand-500) text-white px-3.5 py-2">
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

      <div className="p-3 space-y-3 min-h-0 overflow-y-auto">
        <Seccion titulo="Pedidos (según criticidad)">
          <Item icono={<Pin color={COLOR.verde} />} nombre="A tiempo" detalle="Pedido con holgura suficiente (semáforo verde)" />
          <Item icono={<Pin color={COLOR.ambar} />} nombre="En riesgo" detalle="Pedido con poca holgura (semáforo ámbar)" />
          <Item icono={<Pin color={COLOR.rojo} />} nombre="Crítico" detalle="Pedido por vencer o vencido (semáforo rojo)" />
          <Item
            icono={<Pin color={COLOR.verde} atenuado />}
            nombre="Por atender"
            detalle="Operación día a día: pedido que aún no tiene vehículo asignado (se ve atenuado)"
          />
        </Seccion>
        <p className="-mt-1.5 px-1 text-[11px] text-(--color-ink-500)">
          El número sobre el pin es el código corto del pedido (ej. <b>573</b> = PED-573-…).
        </p>

        <Seccion titulo="Almacenes">
          <Item icono={<Almacen color={COLOR.central} letra="C" />} nombre="Central" detalle="Almacén Central (C): punto principal de abastecimiento, stock ilimitado" />
          <Item icono={<Almacen color={COLOR.norte} letra="N" />} nombre="Norte" detalle="Almacén Norte (N): almacén intermedio, se recarga a las 23:59" />
          <Item icono={<Almacen color={COLOR.este} letra="E" />} nombre="Este" detalle="Almacén Este (E): almacén intermedio, se recarga a las 23:59" />
        </Seccion>

        <Seccion titulo="Vehículos (vistos desde arriba)">
          <Item icono={<Vehiculo tipo="Auto" color={COLOR.auto} />} nombre="Auto" detalle="El frente apunta hacia donde avanza (ej. TA01)" />
          <Item icono={<Vehiculo tipo="Moto" color={COLOR.moto} />} nombre="Moto" detalle="El frente apunta hacia donde avanza (ej. TM01)" />
          <Item icono={<Vehiculo tipo="Bicicleta" color={COLOR.bici} />} nombre="Bicicleta" detalle="El frente apunta hacia donde avanza (ej. TB01)" />
        </Seccion>


        <Seccion titulo="Rutas (color de su vehículo)">
          <Item icono={<LineaRuta color={COLOR.auto} />} nombre="Ruta de auto" detalle="Camino que le falta recorrer a un auto, calle por calle" />
          <Item icono={<LineaRuta color={COLOR.moto} />} nombre="Ruta de moto" detalle="Camino que le falta recorrer a una moto, calle por calle" />
          <Item icono={<LineaRuta color={COLOR.bici} />} nombre="Ruta de bicicleta" detalle="Camino que le falta recorrer a una bicicleta, calle por calle" />
          <Item
            icono={<LineaRuta color={COLOR.auto} seleccionada />}
            nombre="Seleccionada"
            detalle="Al hacer clic en un vehículo su ruta se ve gruesa y las demás se atenúan"
          />
        </Seccion>

        <Seccion titulo="Incidencias">
          <Item
            icono={
              <svg viewBox="0 0 28 14" className="w-6 h-4" aria-hidden="true">
                <line x1="1" y1="7" x2="27" y2="7" stroke="#111827" strokeWidth="2.5" strokeLinecap="round" />
                <rect x="8" y="2.5" width="12" height="9" rx="1.5" fill="white" stroke="#111827" strokeWidth="1" />
                <path d="M10 11 L13 3 M15 11 L18 3" stroke="#DC2626" strokeWidth="1.8" />
              </svg>
            }
            nombre="Bloqueo"
            detalle="Bloqueo en vía: tramo no transitable mientras dure el bloqueo"
          />
          {AVERIAS.map((t) => (
            <Item
              key={t.numero}
              icono={
                <span
                  className="w-5 h-5 rounded-full border border-[#111827] flex items-center justify-center shadow-sm text-[11px] font-bold"
                  style={{ backgroundColor: t.color, color: t.texto }}
                >
                  {t.numero}
                </span>
              }
              nombre={t.nombre}
              detalle={`Avería ${t.detalle}. Va sobre el vehículo, que se ve atenuado.`}
            />
          ))}
        </Seccion>
        {/* Escala de riesgo de las averías */}
        <div className="-mt-1.5 px-1">
          <div className="flex h-1.5 rounded-full overflow-hidden">
            {AVERIAS.map((t) => <div key={t.numero} className="flex-1" style={{ backgroundColor: t.color }} />)}
          </div>
          <div className="flex justify-between text-[10px] text-(--color-ink-500) mt-0.5">
            <span>1 · riesgo bajo</span>
            <span>2 · medio</span>
            <span>3 · alto</span>
          </div>
        </div>

        {/* Cómo usar el mapa */}
        <div className="flex gap-2 rounded-lg bg-gray-50 px-2.5 py-2 text-[11px] text-(--color-ink-500)">
          <MousePointerClick className="w-4 h-4 shrink-0 text-(--color-brand-500)" />
          <span>
            <b className="text-(--color-ink-700)">Clic</b> en un vehículo, almacén o pedido para ver su detalle.{' '}
            <b className="text-(--color-ink-700)">Clic en el mapa vacío</b> o <b className="text-(--color-ink-700)">Esc</b> para cerrarlo.
            Rueda del mouse para acercar y arrastra para moverte.
          </span>
        </div>
      </div>
    </div>
  );
};