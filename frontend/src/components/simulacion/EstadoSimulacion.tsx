import React from 'react';
import { AlertCircle } from 'lucide-react';
import { RelojSimuladoData } from '../../types';

interface EstadoSimulacionProps {
  reloj: RelojSimuladoData;
}

export const EstadoSimulacion: React.FC<EstadoSimulacionProps> = ({ reloj }) => {
  // Parsear fecha y hora
  const fechaTexto = '12/04/2026';
  const horaTexto = reloj.horaSimulada || '10:03:00';
  const diaTexto = `${reloj.diaSimulado || 2} / ${reloj.totalDias || 5}`;
  const pedidosTexto = `${reloj.pedidosEnCurso || 200} u`;

  return (
    <div className="bg-[#b9ccf9] rounded-xl p-3 text-blue-950 shadow-xs border border-blue-300 select-none">
      {/* Estado superior con icono */}
      <div className="flex items-center gap-1.5 font-bold text-xs text-blue-900 mb-2">
        <AlertCircle className="w-3.5 h-3.5 text-blue-700" />
        <span>
          {reloj.estadoEjecucion === 'EN_EJECUCION'
            ? 'Simulación en ejecución'
            : reloj.estadoEjecucion === 'FINALIZADA'
            ? 'Simulación finalizada'
            : 'Simulación pausada'}
        </span>
      </div>

      {/* Cuadrícula de datos en 2 columnas */}
      <div className="grid grid-cols-2 gap-y-1 gap-x-2 text-[11px]">
        <div className="flex items-center gap-1">
          <span className="font-semibold text-blue-900">Día:</span>
          <span className="font-bold text-blue-950">{diaTexto}</span>
        </div>

        <div className="flex items-center gap-1">
          <span className="font-semibold text-blue-900">Ped. en curso:</span>
          <span className="font-bold text-blue-950">{pedidosTexto}</span>
        </div>

        <div className="flex items-center gap-1">
          <span className="font-semibold text-blue-900">Fecha:</span>
          <span className="font-bold text-blue-950">{fechaTexto}</span>
        </div>

        <div className="flex items-center gap-1">
          <span className="font-semibold text-blue-900">Hor. pasada:</span>
          <span className="font-bold text-blue-950">{horaTexto}</span>
        </div>
      </div>
    </div>
  );
};
