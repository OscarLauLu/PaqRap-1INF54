import React, { useState } from 'react';
import { Almacen, UnidadTransporte, Pedido } from '../../types';
import { LeyendaMapa } from './LeyendaMapa';
import { Car, Bike } from 'lucide-react';

interface MapaOperacionesProps {
  almacenes: Almacen[];
  unidades: UnidadTransporte[];
  pedidos: Pedido[];
  selectedUnidadId: number | null;
  onSelectUnidad: (id: number) => void;
}

export const MapaOperaciones: React.FC<MapaOperacionesProps> = ({
  almacenes,
  unidades,
  pedidos,
  selectedUnidadId,
  onSelectUnidad,
}) => {
  const [showLeyenda, setShowLeyenda] = useState(false);

  // Dimensiones del mapa: 70 x 50 km convertido a escala 10x (700 x 500 px)
  const scale = 10;
  const mapWidth = 70 * scale;
  const mapHeight = 50 * scale;

  // Conversión de coordenadas: (x, y) en km a píxeles SVG (Y invertida para que Y mayor esté arriba)
  const toSvgX = (xKm: number) => xKm * scale;
  const toSvgY = (yKm: number) => (50 - yKm) * scale;

  // Generación de cuadrícula de fondo cada 5 km
  const gridLinesX = [];
  for (let x = 5; x < 70; x += 5) {
    gridLinesX.push(x * scale);
  }
  const gridLinesY = [];
  for (let y = 5; y < 50; y += 5) {
    gridLinesY.push(y * scale);
  }

  return (
    <div className="relative bg-[#ebf1f6] rounded-2xl border border-gray-300/80 p-3 shadow-inner overflow-hidden select-none">
      {/* SVG Canvas */}
      <svg
        viewBox={`0 0 ${mapWidth} ${mapHeight}`}
        className="w-full h-auto aspect-70/50 block"
      >
        {/* Cuadrícula ortogonal */}
        <g stroke="#dbe4ec" strokeWidth="1.5">
          {gridLinesX.map((x) => (
            <line key={`gx-${x}`} x1={x} y1={0} x2={x} y2={mapHeight} />
          ))}
          {gridLinesY.map((y) => (
            <line key={`gy-${y}`} x1={0} y1={y} x2={mapWidth} y2={y} />
          ))}
        </g>

        {/* 1. Rutas trazadas (Normales: Azul punteada, Alternativas: Amarilla punteada, Bloqueadas: Roja) */}
        <g strokeWidth="2.5" strokeDasharray="5,5" fill="none">
          {/* Ruta Normal azul: Intermedio N-O hacia el sur y luego hacia almacén central */}
          <polyline
            points="170,130 240,130 240,260 280,260 280,330"
            stroke="#2563EB"
          />
          <polyline
            points="280,330 280,390 380,390"
            stroke="#2563EB"
          />
          <polyline
            points="380,280 430,280 430,350 480,350 480,260 580,260"
            stroke="#2563EB"
          />
          <polyline
            points="480,260 480,180 570,180"
            stroke="#2563EB"
          />

          {/* Rutas Verdes / Secundarias */}
          <polyline
            points="320,150 360,150 360,280"
            stroke="#16A34A"
          />
          <polyline
            points="480,350 540,350 540,380"
            stroke="#16A34A"
          />

          {/* Rutas Alternativas amarillas */}
          <polyline
            points="380,280 420,280 420,180 480,180 480,320"
            stroke="#EAB308"
          />

          {/* Calle bloqueada (Roja punteada) */}
          <line
            x1={200}
            y1={390}
            x2={280}
            y2={390}
            stroke="#DC2626"
            strokeWidth="3"
          />
          <line
            x1={480}
            y1={140}
            x2={480}
            y2={220}
            stroke="#DC2626"
            strokeWidth="3"
          />
        </g>

        {/* 2. Marcadores de Clientes / Pedidos (P1, P2, P3, P4, P5, P6, P7) */}
        {pedidos.map((p) => {
          const px = toSvgX(p.destino.x);
          const py = toSvgY(p.destino.y);
          return (
            <g key={`pedido-${p.id}`} className="cursor-pointer group">
              <circle
                cx={px}
                cy={py}
                r={6}
                fill="#64748B"
                className="group-hover:fill-blue-600 transition-colors"
              />
              <text
                x={px}
                y={py + 15}
                textAnchor="middle"
                className="text-[9px] font-bold fill-gray-600 select-none"
              >
                {p.codigo}
              </text>
            </g>
          );
        })}

        {/* 3. Incidencias en el Mapa */}
        {/* Bloqueo 1 en (28, 17) */}
        <g transform="translate(280, 330)">
          <circle r="9" fill="#DC2626" />
          <text textAnchor="middle" dy="3.5" fill="white" fontSize="10" fontWeight="bold">✕</text>
        </g>
        {/* Bloqueo 2 en (23, 11) */}
        <g transform="translate(230, 390)">
          <circle r="9" fill="#DC2626" />
          <text textAnchor="middle" dy="3.5" fill="white" fontSize="10" fontWeight="bold">✕</text>
        </g>
        {/* Bloqueo 3 en (48, 36) */}
        <g transform="translate(480, 140)">
          <circle r="9" fill="#DC2626" />
          <text textAnchor="middle" dy="3.5" fill="white" fontSize="10" fontWeight="bold">✕</text>
        </g>

        {/* Avería mecánica en (53, 32) */}
        <g transform="translate(530, 180)">
          <circle r="12" fill="#FEE2E2" stroke="#EF4444" strokeWidth="1.5" />
          <path
            d="M -6,5 L 6,5 L 0,-6 Z"
            fill="#DC2626"
          />
          <text textAnchor="middle" dy="4" fill="white" fontSize="8" fontWeight="black">!</text>
        </g>

        {/* 4. Almacenes en el Mapa */}
        {almacenes.map((alm) => {
          const ax = toSvgX(alm.ubicacion.x);
          const ay = toSvgY(alm.ubicacion.y);
          const letra = alm.tipo === 'CENTRAL' ? 'C' : alm.nombre.includes('N-O') ? 'N' : 'E';

          return (
            <g key={`alm-${alm.id}`} transform={`translate(${ax}, ${ay})`} className="cursor-pointer">
              {/* Tarjeta del almacén */}
              <rect
                x="-14"
                y="-14"
                width="28"
                height="28"
                rx="6"
                fill="#1D4ED8"
                stroke="#1E40AF"
                strokeWidth="1.5"
                filter="drop-shadow(0 2px 4px rgba(0,0,0,0.15))"
              />
              {/* Techo de almacén */}
              <path
                d="M -10 -3 L 0 -10 L 10 -3"
                stroke="white"
                strokeWidth="1.5"
                fill="none"
              />
              <text
                x="0"
                y="8"
                textAnchor="middle"
                fill="white"
                fontSize="11"
                fontWeight="bold"
              >
                {letra}
              </text>
              {/* Nombre debajo */}
              <text
                x="0"
                y="24"
                textAnchor="middle"
                fill="#1E3A8A"
                fontSize="9"
                fontWeight="bold"
              >
                {alm.tipo === 'CENTRAL' ? 'Central' : alm.nombre}
              </text>
            </g>
          );
        })}

        {/* 5. Unidades de Transporte (Vehículos) */}
        {unidades.map((u) => {
          const ux = toSvgX(u.ubicacionActual.x);
          const uy = toSvgY(u.ubicacionActual.y);
          const isSelected = u.id === selectedUnidadId;

          // Colores según tipo
          const bgColor =
            u.tipoNombre === 'Auto'
              ? '#DC2626'
              : u.tipoNombre === 'Moto'
              ? u.id === 4
                ? '#2563EB'
                : '#16A34A'
              : '#EAB308';

          return (
            <g
              key={`vehiculo-${u.id}`}
              transform={`translate(${ux}, ${uy})`}
              onClick={() => onSelectUnidad(u.id)}
              className="cursor-pointer group"
            >
              {/* Halo de selección animado si está seleccionado */}
              {isSelected && (
                <circle
                  r="18"
                  fill="none"
                  stroke="#22C55E"
                  strokeWidth="2.5"
                  className="animate-pulse"
                />
              )}

              {/* Círculo base de la unidad */}
              <circle
                r="12"
                fill="white"
                stroke={bgColor}
                strokeWidth="2.5"
                className="group-hover:scale-110 transition-transform"
              />

              {/* Icono de vehículo */}
              {u.tipoNombre === 'Auto' ? (
                <Car className="w-3.5 h-3.5" x="-7" y="-7" stroke={bgColor} strokeWidth="2.5" />
              ) : (
                <Bike className="w-3.5 h-3.5" x="-7" y="-7" stroke={bgColor} strokeWidth="2.5" />
              )}
            </g>
          );
        })}
      </svg>

      {/* Rosa de los vientos (Norte) en la esquina superior derecha */}
      <div className="absolute top-6 right-6 flex flex-col items-center select-none pointer-events-none opacity-80">
        <span className="text-[11px] font-black text-gray-700 leading-none mb-0.5">N</span>
        <svg className="w-4 h-6 fill-gray-700" viewBox="0 0 24 36">
          <polygon points="12,0 20,20 12,14 4,20" />
        </svg>
      </div>

      {/* Escala (5 km) en la esquina inferior izquierda */}
      <div className="absolute bottom-5 left-5 flex flex-col items-center select-none pointer-events-none">
        <div className="w-16 h-1.5 border-x-2 border-b-2 border-gray-600"></div>
        <span className="text-[10px] font-semibold text-gray-600 mt-1">5 km</span>
      </div>

      {/* Botón flotante para alternar la Leyenda */}
      <div className="absolute bottom-4 right-4">
        <button
          onClick={() => setShowLeyenda(!showLeyenda)}
          className="bg-white/95 hover:bg-white text-gray-700 font-semibold px-4 py-1.5 rounded-lg border border-gray-300 text-xs shadow-md transition-all cursor-pointer"
        >
          Leyenda
        </button>
      </div>

      {/* Panel flotante de Leyenda cuando está activo */}
      {showLeyenda && (
        <div className="absolute bottom-14 right-4 z-20">
          <LeyendaMapa onClose={() => setShowLeyenda(false)} />
        </div>
      )}
    </div>
  );
};
