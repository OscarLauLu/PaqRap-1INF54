import React from 'react';
import { IncidenteItem } from '../../types';

interface HistorialIncidentesProps {
  incidentes: IncidenteItem[];
  loading?: boolean;
}

export const HistorialIncidentes: React.FC<HistorialIncidentesProps> = ({
  incidentes,
  loading,
}) => {
  return (
    <div className="mt-6">
      <h2 className="text-base font-bold text-gray-800 mb-3 tracking-tight">Historial de incidentes</h2>

      <div className="bg-white rounded-xl border border-blue-400 overflow-hidden shadow-xs">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="border-b border-blue-300 text-blue-600 font-bold">
                <th className="py-2.5 px-4 w-12 text-center">N</th>
                <th className="py-2.5 px-4">Evento</th>
                <th className="py-2.5 px-4">Ubicación</th>
                <th className="py-2.5 px-4">Impacto</th>
                <th className="py-2.5 px-4 text-center">Día</th>
                <th className="py-2.5 px-4 text-center">Hora</th>
                <th className="py-2.5 px-4 text-center">Estado</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 font-medium text-gray-700">
              {incidentes.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-4 text-center text-gray-400">
                    No se han registrado incidencias en la simulación.
                  </td>
                </tr>
              ) : (
                incidentes.map((item) => {
                  const isResuelta = item.estado === 'Resuelta';

                  return (
                    <tr key={item.id} className="hover:bg-blue-50/30 transition-colors">
                      <td className="py-3 px-4 text-center font-bold text-gray-800">
                        {item.numero}
                      </td>
                      <td className="py-3 px-4 text-gray-800">
                        <span className="font-semibold">{item.evento.split(':')[0]}:</span>
                        <span className="text-gray-600 font-normal">
                          {item.evento.split(':')[1] || ''}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-gray-600">{item.ubicacion}</td>
                      <td className="py-3 px-4 text-gray-600">{item.impacto}</td>
                      <td className="py-3 px-4 text-center text-gray-700">{item.dia}</td>
                      <td className="py-3 px-4 text-center text-gray-600 font-mono text-[11px]">
                        {item.hora}
                      </td>
                      <td className="py-3 px-4 text-center">
                        <span
                          className={`font-semibold text-xs ${
                            isResuelta ? 'text-amber-600' : 'text-emerald-600'
                          }`}
                        >
                          {item.estado}
                        </span>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
