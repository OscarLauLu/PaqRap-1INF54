import React from 'react';
import { Infinity as InfinityIcon } from 'lucide-react';
import { Almacen } from '../../types';

interface StockAlmacenesProps {
  almacenes: Almacen[];
  loading?: boolean;
}

export const StockAlmacenes: React.FC<StockAlmacenesProps> = ({ almacenes, loading }) => {
  return (
    <div className="mb-6">
      <h2 className="text-base font-bold text-gray-800 mb-3 tracking-tight">Stock de almacenes</h2>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {almacenes.map((almacen) => {
          const isCentral = almacen.tipo === 'CENTRAL';
          const porcentaje = isCentral ? 0 : almacen.porcentajeOcupacion;

          return (
            <div
              key={almacen.id}
              className="bg-white rounded-xl border border-blue-300 p-4 shadow-xs hover:border-blue-500 transition-all flex flex-col justify-between min-h-[90px]"
            >
              <div className="flex items-start justify-between">
                <span className="font-semibold text-gray-700 text-sm">{almacen.nombre}</span>
                <span className="text-xs text-gray-400 font-medium">
                  {isCentral ? `${almacen.stockActual} u` : `${almacen.stockActual} / ${almacen.capacidadMaxima || 1000} u`}
                </span>
              </div>

              <div className="mt-3">
                {isCentral ? (
                  <div className="flex items-center gap-1.5 text-blue-600 font-semibold text-xs">
                    <InfinityIcon className="w-4 h-4 stroke-[2.5]" />
                    <span>Ilimitado</span>
                  </div>
                ) : (
                  <div className="flex items-center gap-3">
                    <div className="flex-1 bg-gray-100 rounded-full h-2 overflow-hidden">
                      <div
                        className={`h-full rounded-full transition-all duration-500 ${
                          porcentaje > 90 ? 'bg-blue-600' : 'bg-blue-600'
                        }`}
                        style={{ width: `${Math.min(porcentaje, 100)}%` }}
                      />
                    </div>
                    <span className="text-[11px] font-bold text-gray-700 w-10 text-right">
                      {porcentaje.toFixed(1)}%
                    </span>
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
