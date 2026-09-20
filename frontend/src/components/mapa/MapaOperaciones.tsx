import React, { useState } from 'react';
import { Almacen, Pedido, UnidadTransporte } from '../../types';
import { Car, Bike } from 'lucide-react';
import { LeyendaMapa } from './LeyendaMapa';

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

  // La matriz tiene 70 de ancho por 50 de alto.
  // Usaremos viewBox="0 0 70 50" para que 1 unidad SVG = 1 km.
  // Y como el origen en el backend está abajo a la izquierda, y SVG está arriba a la izquierda:
  const toSvgX = (x: number) => x;
  const toSvgY = (y: number) => 50 - y;

  return (
    <div className="relative w-full h-[600px] bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
      <svg
        className="w-full h-full bg-[#f8fafc]"
        viewBox="-2 -2 74 54"
        preserveAspectRatio="xMidYMid meet"
      >
        {/* Renderizado manual de la cuadrícula exacta */}
        <g stroke="#CBD5E1" strokeWidth="0.05">
          {Array.from({ length: 71 }).map((_, i) => (
            <line key={`v-${i}`} x1={i} y1={0} x2={i} y2={50} />
          ))}
          {Array.from({ length: 51 }).map((_, i) => (
            <line key={`h-${i}`} x1={0} y1={i} x2={70} y2={i} />
          ))}
        </g>

        {/* Marcadores de Clientes / Pedidos */}
        {pedidos.map((p) => {
          if (!p.destino) return null;
          const px = toSvgX(p.destino.posX);
          const py = toSvgY(p.destino.posY);
          return (
            <g key={`pedido-${p.id}`} className="cursor-pointer group">
              <circle cx={px} cy={py} r={0.6} fill="#2563EB" />
              <text x={px} y={py - 1} textAnchor="middle" fontSize="0.6" fontWeight="bold" fill="#2563EB" className="select-none">
                {p.codigo}
              </text>
            </g>
          );
        })}

        {/* Almacenes */}
        {almacenes.map((alm) => {
          const ax = toSvgX(alm.ubicacion.posX);
          const ay = toSvgY(alm.ubicacion.posY);
          const letra = alm.tipo === 'CENTRAL' ? 'C' : alm.nombre.includes('Norte') ? 'N' : 'E';
          const color = alm.tipo === 'CENTRAL' ? '#DC2626' : alm.nombre.includes('Norte') ? '#F97316' : '#9333EA';

          return (
            <g key={`alm-${alm.id}`} transform={`translate(${ax}, ${ay})`} className="cursor-pointer">
              <rect x="-1.5" y="-1.5" width="3" height="3" rx="0.5" fill={color} />
              <path d="M -1 -0.2 L 0 -1.2 L 1 -0.2" stroke="white" strokeWidth="0.2" fill="none" />
              <text x="0" y="1" textAnchor="middle" fill="white" fontSize="1.5" fontWeight="bold">{letra}</text>
            </g>
          );
        })}

        {/* Unidades de Transporte */}
        {unidades.map((u) => {
          const ux = toSvgX(u.ubicacionActual.posX);
          const uy = toSvgY(u.ubicacionActual.posY);
          const isSelected = u.id === selectedUnidadId;
          const bgColor = u.tipoNombre === 'Auto' ? '#3B82F6' : u.tipoNombre === 'Moto' ? '#16A34A' : '#EAB308';
          const shortCode = u.tipoNombre.charAt(0) + u.id;

          return (
            <g key={`vehiculo-${u.id}`} transform={`translate(${ux}, ${uy})`} onClick={() => onSelectUnidad(u.id)} className="cursor-pointer group">
              {isSelected && <circle r="2.5" fill="none" stroke="#60A5FA" strokeWidth="0.3" className="animate-pulse" />}
              {u.tipoNombre === 'Auto' ? (
                <Car width={2.5} height={2.5} x="-1.25" y="-1.25" stroke={bgColor} strokeWidth={2} />
              ) : (
                <Bike width={2.5} height={2.5} x="-1.25" y="-1.25" stroke={bgColor} strokeWidth={2} />
              )}
              <text x="0" y="2" textAnchor="middle" fontSize="1" fontWeight="bold" fill="#1F2937">{shortCode}</text>
            </g>
          );
        })}
      </svg>

      {/* Rosa de los vientos (Norte) */}
      <div className="absolute top-4 left-4 flex flex-col items-center select-none pointer-events-none opacity-80">
        <span className="text-xs font-black text-gray-800 leading-none mb-0.5">N</span>
        <svg className="w-3 h-5 fill-gray-800" viewBox="0 0 24 36"><polygon points="12,0 20,20 12,14 4,20" /></svg>
      </div>

      <div className="absolute bottom-4 right-4">
        <button onClick={() => setShowLeyenda(!showLeyenda)} className="bg-white/95 hover:bg-white text-gray-700 font-semibold px-4 py-1.5 rounded-lg border border-gray-300 text-xs shadow-md transition-all cursor-pointer">
          Leyenda
        </button>
      </div>
      {showLeyenda && <div className="absolute bottom-14 right-4 z-20"><LeyendaMapa onClose={() => setShowLeyenda(false)} /></div>}
    </div>
  );
};
