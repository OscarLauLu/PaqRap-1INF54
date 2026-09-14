import React from 'react';
import {
  Bike,
  Car,
  Package,
  Gauge,
  User,
  Clock,
  ChevronDown,
  CircleDot,
  DollarSign,
} from 'lucide-react';
import { UnidadTransporte } from '../../types';

interface DetalleUnidadProps {
  unidad: UnidadTransporte | null;
}

export const DetalleUnidad: React.FC<DetalleUnidadProps> = ({ unidad }) => {
  if (!unidad) {
    return (
      <div className="bg-white rounded-2xl border border-gray-200 p-5 shadow-xs">
        <h3 className="font-bold text-gray-800 text-sm mb-3">Detalles de la unidad</h3>
        <p className="text-xs text-gray-400">Selecciona un vehículo en el mapa para ver sus detalles.</p>
      </div>
    );
  }

  const isCar = unidad.tipoNombre === 'Auto';
  const isBike = unidad.tipoNombre === 'Bicicleta';
  const tipoLimpio = isCar ? 'Automóvil' : isBike ? 'Bicicleta' : 'Motocicleta';

  return (
    <div className="bg-white rounded-2xl border border-gray-200 p-5 shadow-xs flex flex-col justify-between select-none">
      <div>
        <h3 className="font-bold text-gray-800 text-sm mb-4">Detalles de la unidad</h3>

        {/* Cabecera del vehículo */}
        <div className="flex items-center justify-between mb-4 pb-3 border-b border-gray-100">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 bg-blue-100/80 rounded-xl flex items-center justify-center text-blue-800">
              {isCar ? (
                <Car className="w-6 h-6 stroke-[1.8]" />
              ) : (
                <Bike className="w-6 h-6 stroke-[1.8]" />
              )}
            </div>
            <div>
              <h4 className="font-bold text-gray-900 text-base leading-tight">{unidad.codigo}</h4>
              <span className="text-xs text-gray-400 font-medium">{tipoLimpio}</span>
            </div>
          </div>

          <span className="text-xs font-semibold text-blue-600 bg-blue-50 px-2.5 py-1 rounded-full border border-blue-200">
            {unidad.cargaActual > 0 ? 'Con pedido' : 'Disponible'}
          </span>
        </div>

        {/* Lista de atributos técnicos y operativos */}
        <div className="space-y-2 text-xs">
          {/* Pedido asignado */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-gray-500 font-medium">
              <CircleDot className="w-3.5 h-3.5 text-blue-500" />
              <span>Pedido asignado</span>
            </div>
            <span className="font-semibold text-gray-800">{unidad.pedidoAsignado || 'PED-2854'}</span>
          </div>

          {/* Carga actual */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-gray-500 font-medium">
              <Package className="w-3.5 h-3.5 text-blue-500" />
              <span>Carga actual</span>
            </div>
            <span className="font-semibold text-gray-800">
              {unidad.cargaActual} / {unidad.capacidadMaxima}
            </span>
          </div>

          {/* Capacidad */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-gray-500 font-medium">
              <span className="w-3.5 text-center text-gray-400">⚖</span>
              <span>Capacidad</span>
            </div>
            <span className="font-semibold text-gray-800">{unidad.capacidadMaxima} paquetes</span>
          </div>

          {/* Velocidad */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-gray-500 font-medium">
              <Gauge className="w-3.5 h-3.5 text-blue-500" />
              <span>Velocidad</span>
            </div>
            <span className="font-semibold text-gray-800">{unidad.velocidadPromedioKmH} km/h</span>
          </div>

          {/* Costo */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-gray-500 font-medium">
              <DollarSign className="w-3.5 h-3.5 text-blue-500" />
              <span>Costo</span>
            </div>
            <span className="font-semibold text-gray-800">S/ {unidad.costoPorKm.toFixed(2)}/km</span>
          </div>

          {/* Conductor */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-gray-500 font-medium">
              <User className="w-3.5 h-3.5 text-blue-500" />
              <span>Conductor</span>
            </div>
            <span className="font-semibold text-gray-800">{unidad.nombreConductor || 'Juan Pérez'}</span>
          </div>

          {/* Turno */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-gray-500 font-medium">
              <Clock className="w-3.5 h-3.5 text-blue-500" />
              <span>Turno</span>
            </div>
            <span className="font-semibold text-gray-800">{unidad.turno || '07:00–15:00'}</span>
          </div>

          {/* Ocupación */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-gray-500 font-medium">
              <span className="w-3.5 text-center text-blue-500">%</span>
              <span>Ocupación</span>
            </div>
            <span className="font-semibold text-gray-800">{unidad.porcentajeCarga.toFixed(0)}%</span>
          </div>
        </div>
      </div>

      {/* Flecha inferior colapsable */}
      <div className="pt-3 mt-3 border-t border-gray-100 flex justify-center">
        <button className="text-blue-500 hover:text-blue-700 p-1 rounded-full hover:bg-blue-50 transition-colors">
          <ChevronDown className="w-5 h-5" />
        </button>
      </div>
    </div>
  );
};
