import React from 'react';
import { X, Car, Bike } from 'lucide-react';


interface LeyendaMapaProps {
  onClose?: () => void;
}

export const LeyendaMapa: React.FC<LeyendaMapaProps> = ({ onClose }) => {
  return (
    <div className="bg-white/95 backdrop-blur-xs border border-gray-200 rounded-xl p-4 shadow-xl text-xs w-64 select-none animate-in fade-in zoom-in duration-150">
      <div className="flex items-center justify-between pb-2 border-b border-gray-100 mb-3">
        <h3 className="font-bold text-gray-800 text-sm">Leyenda</h3>
        {onClose && (
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 p-0.5 rounded-sm hover:bg-gray-100"
          >
            <X className="w-4 h-4" />
          </button>
        )}
      </div>

      {/* Almacenes */}
      <div className="mb-3">
        <span className="font-semibold text-gray-500 uppercase tracking-wider text-[10px] block mb-1.5">
          Almacenes
        </span>
        <div className="space-y-1.5">
          <div className="flex items-center gap-2 text-gray-700">
            <div className="w-5 h-5 bg-blue-700 text-white rounded-md flex items-center justify-center font-bold text-[10px]">
              C
            </div>
            <span>Almacén Central</span>
          </div>
          <div className="flex items-center gap-2 text-gray-700">
            <div className="w-5 h-5 bg-blue-700 text-white rounded-md flex items-center justify-center font-bold text-[10px]">
              N
            </div>
            <span>Almacén Intermedio N-O</span>
          </div>
          <div className="flex items-center gap-2 text-gray-700">
            <div className="w-5 h-5 bg-blue-700 text-white rounded-md flex items-center justify-center font-bold text-[10px]">
              E
            </div>
            <span>Almacén Intermedio Este</span>
          </div>
        </div>
      </div>

      {/* Vehículos */}
      <div className="mb-3">
        <span className="font-semibold text-gray-500 uppercase tracking-wider text-[10px] block mb-1.5">
          Vehículos
        </span>
        <div className="space-y-1.5">
          <div className="flex items-center gap-2 text-gray-700">
            <div className="w-5 h-5 bg-red-500 text-white rounded-full flex items-center justify-center">
              <Car className="w-3 h-3" />
            </div>
            <span>Auto</span>
          </div>
          <div className="flex items-center gap-2 text-gray-700">
            <div className="w-5 h-5 bg-emerald-600 text-white rounded-full flex items-center justify-center">
              <Bike className="w-3 h-3" />
            </div>
            <span>Moto</span>
          </div>
          <div className="flex items-center gap-2 text-gray-700">
            <div className="w-5 h-5 bg-amber-500 text-white rounded-full flex items-center justify-center">
              <Bike className="w-3 h-3" />
            </div>
            <span>Bicicleta</span>
          </div>
        </div>
      </div>

      {/* Incidencias */}
      <div className="mb-3">
        <span className="font-semibold text-gray-500 uppercase tracking-wider text-[10px] block mb-1.5">
          Incidencias
        </span>
        <div className="space-y-1.5">
          <div className="flex items-center gap-2 text-gray-700">
            <div className="w-5 h-5 bg-red-600 text-white rounded-full flex items-center justify-center font-bold text-[10px]">
              ✕
            </div>
            <span>Bloqueo de calle</span>
          </div>
          <div className="flex items-center gap-2 text-gray-700">
            <div className="w-5 h-5 bg-red-500 text-white rounded-xs flex items-center justify-center font-bold text-[10px]">
              ▲
            </div>
            <span>Avería de unidad</span>
          </div>
        </div>
      </div>

      {/* Rutas */}
      <div className="mb-3">
        <span className="font-semibold text-gray-500 uppercase tracking-wider text-[10px] block mb-1.5">
          Rutas
        </span>
        <div className="space-y-1.5">
          <div className="flex items-center gap-2 text-gray-700">
            <div className="w-6 h-0.5 border-t-2 border-dashed border-blue-600"></div>
            <span>Ruta de unidad (normal)</span>
          </div>
          <div className="flex items-center gap-2 text-gray-700">
            <div className="w-6 h-0.5 border-t-2 border-dashed border-amber-500"></div>
            <span>Ruta de unidad (alternativa)</span>
          </div>
          <div className="flex items-center gap-2 text-gray-700">
            <div className="w-6 h-0.5 border-t-2 border-dashed border-red-500"></div>
            <span>Calle bloqueada</span>
          </div>
        </div>
      </div>

      {/* Otros */}
      <div>
        <span className="font-semibold text-gray-500 uppercase tracking-wider text-[10px] block mb-1.5">
          Otros
        </span>
        <div className="flex items-center gap-2 text-gray-700">
          <div className="w-3.5 h-3.5 bg-gray-500 rounded-full"></div>
          <span>Cliente / Pedido</span>
        </div>
      </div>
    </div>
  );
};
