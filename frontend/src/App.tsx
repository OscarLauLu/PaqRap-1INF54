import React, { useState } from 'react';
import { Sidebar } from './components/layout/Sidebar';
import { Header } from './components/layout/Header';
import { MapaOperaciones } from './components/mapa/MapaOperaciones';
import { RightPanel } from './components/panel/RightPanel';
import { Play, Pause, Square } from 'lucide-react';

import { useAlmacenes } from './hooks/useAlmacenes';
import { useSimulacion } from './hooks/useSimulacion';
import { useFlota } from './hooks/useFlota';
import { useMapaOperaciones } from './hooks/useMapaOperaciones';

export const App: React.FC = () => {
  const [activeSection, setActiveSection] = useState('simulacion');
  const [speed, setSpeed] = useState(1);

  const { almacenes } = useAlmacenes(1000);
  const { reloj, iniciar, detener, finalizarSimulacion } = useSimulacion(1000);
  const { unidades, selectedUnidadId, setSelectedUnidadId } = useFlota(1000);
  const { pedidos } = useMapaOperaciones(1000);

  const formatFecha = (isoString: string) => {
    if (!isoString) return '';
    const date = new Date(isoString);
    return date.toLocaleString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  };
  
  const getTranscurrido = () => {
    if (!reloj.instanteActual) return '0 h';
    const hours = (reloj.diaSimulado - 1) * 24 + parseInt(reloj.horaSimulada.split(':')[0] || '0');
    return `${hours} h`;
  };

  const diaActual = reloj.diaSimulado || 1;

  return (
    <div className="flex min-h-screen bg-[#f8fafc] text-gray-800">
      <Sidebar activeSection={activeSection} onSelectSection={setActiveSection} />

      <div className="flex-1 flex flex-col min-w-0">
        <Header onFinalizarSimulacion={finalizarSimulacion} totalAlertas={0} isSimulating={reloj.estadoEjecucion === 'EN_EJECUCION'} userName="Jones Ferdinand" />

        <main className="flex-1 p-6 md:p-8 flex flex-col w-full h-[calc(100vh-72px)] overflow-hidden">
          
          {/* Top Bar - Controls */}
          <div className="flex justify-between items-center mb-4 text-sm font-semibold text-gray-600 bg-white p-3 rounded-xl border border-gray-100 shadow-sm">
            <div className="flex items-center space-x-8 px-4">
              <div>Fecha: <span className="text-gray-900 ml-1">{formatFecha(reloj.instanteActual)}</span></div>
              <div className="flex items-center"><ClockIcon className="w-4 h-4 mr-1"/> Tiempo transcurrido: <span className="text-gray-900 ml-1">{getTranscurrido()}</span></div>
              <div>Día: <span className="text-gray-900 ml-1">{diaActual}</span></div>
            </div>
            
            <div className="flex items-center space-x-2">
              <button onClick={() => iniciar()} className="p-2 bg-blue-50 text-blue-600 rounded-lg border border-blue-100 hover:bg-blue-100 transition-colors"><Play size={18} fill="currentColor" /></button>
              <button onClick={detener} className="p-2 bg-gray-50 text-gray-600 rounded-lg border border-gray-200 hover:bg-gray-100 transition-colors"><Pause size={18} fill="currentColor" /></button>
              <button onClick={finalizarSimulacion} className="p-2 bg-gray-50 text-gray-600 rounded-lg border border-gray-200 hover:bg-gray-100 transition-colors"><Square size={18} fill="currentColor" /></button>
              <select className="bg-white border border-gray-300 text-gray-700 rounded-lg p-1.5 text-sm font-bold ml-2 outline-none">
                <option value="0.5">0.5x</option>
                <option value="1" selected>1.0x</option>
                <option value="2">2.0x</option>
              </select>
            </div>
          </div>

          <div className="flex flex-1 gap-6 min-h-0">
            {/* Mapa (Flex-1) */}
            <div className="flex-1 h-full flex flex-col">
              <MapaOperaciones almacenes={almacenes} unidades={unidades} pedidos={pedidos} selectedUnidadId={selectedUnidadId} onSelectUnidad={setSelectedUnidadId} />
            </div>

            {/* Right Panel (Fixed Width) */}
            <RightPanel reloj={reloj} almacenes={almacenes} unidades={unidades} pedidos={pedidos} selectedUnidadId={selectedUnidadId} />
          </div>

        </main>
      </div>
    </div>
  );
};

const ClockIcon = ({ className }: { className?: string }) => (
  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className={className}><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
);

export default App;
