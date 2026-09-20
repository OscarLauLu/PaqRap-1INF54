import React, { useState } from 'react';
import { Calendar, ChevronDown, ChevronUp, Clock, Package, MapPin, Truck, AlertCircle, TrendingUp, AlertTriangle } from 'lucide-react';
import { RelojSimuladoData, Almacen, UnidadTransporte, Pedido } from '../../types';

interface RightPanelProps {
  reloj: RelojSimuladoData;
  almacenes: Almacen[];
  unidades: UnidadTransporte[];
  pedidos: Pedido[];
  selectedUnidadId: number | null;
}

export const RightPanel: React.FC<RightPanelProps> = ({ reloj, almacenes, unidades, pedidos, selectedUnidadId }) => {
  const [expandedSection, setExpandedSection] = useState<string | null>('fecha');

  const toggleSection = (section: string) => {
    setExpandedSection(prev => prev === section ? null : section);
  };

  const selectedUnidad = selectedUnidadId ? unidades.find(u => u.id === selectedUnidadId) : null;
  const autoCount = unidades.filter(u => u.tipoNombre === 'Auto');
  const motoCount = unidades.filter(u => u.tipoNombre === 'Moto');
  const biciCount = unidades.filter(u => u.tipoNombre === 'Bicicleta');
  
  const getCounts = (list: UnidadTransporte[]) => {
    return {
      disp: list.filter(u => u.estadoOperativo === 'DISPONIBLE').length,
      ruta: list.filter(u => u.estadoOperativo === 'EN_RUTA').length,
      ave: list.filter(u => u.estadoOperativo === 'AVERIADO' || u.estadoOperativo === 'EN_MANTENIMIENTO').length,
      tot: list.length
    };
  };

  const aCount = getCounts(autoCount);
  const mCount = getCounts(motoCount);
  const bCount = getCounts(biciCount);

  const totalPedidos = pedidos.length;
  const entregados = pedidos.filter(p => p.estado === 'ENTREGADO').length;
  const enRuta = pedidos.filter(p => p.estado === 'EN_RUTA' || p.estado === 'PLANIFICADO').length;
  const porAtender = pedidos.filter(p => p.estado === 'REGISTRADO').length;
  const reasignados = 0; // TODO if needed
  
  const pctCumplimiento = totalPedidos > 0 ? (entregados / totalPedidos) * 100 : 0;

  const Accordion = ({ title, id, children }: { title: string, id: string, children: React.ReactNode }) => (
    <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden mb-2">
      <button 
        className="w-full px-4 py-3 flex justify-between items-center text-gray-700 font-semibold hover:bg-gray-50/50"
        onClick={() => toggleSection(id)}
      >
        <span className="text-sm">{title}</span>
        {expandedSection === id ? <ChevronUp size={18} className="text-gray-400" /> : <ChevronDown size={18} className="text-gray-400" />}
      </button>
      {expandedSection === id && (
        <div className="px-4 pb-4 bg-white">
          {children}
        </div>
      )}
    </div>
  );

  return (
    <div className="w-80 flex-shrink-0 space-y-3 overflow-y-auto max-h-screen pr-2 scrollbar-thin">
      
      {/* Fecha Actual */}
      <div className="bg-blue-200/80 rounded-xl shadow-sm border border-blue-300 p-3 flex flex-col items-center justify-center mb-4">
        <div className="flex items-center text-blue-900 font-bold mb-1">
          <Calendar size={16} className="mr-2" /> Fecha actual
        </div>
        <div className="text-blue-900 text-sm">
          {new Date(reloj.instanteActual).toLocaleString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })}
        </div>
      </div>

      <Accordion title="Resumen de pedidos" id="resumen">
        <div className="space-y-3">
          <div className="flex items-center space-x-2">
            <Clock size={20} className="text-gray-700" />
            <span className="font-bold text-gray-800 text-lg">Cumplimiento</span>
            <span className="ml-auto font-bold text-blue-600 text-lg">{pctCumplimiento.toFixed(1)} %</span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-2">
            <div className="bg-blue-600 h-2 rounded-full" style={{ width: `${pctCumplimiento}%` }}></div>
          </div>
          <div className="pt-2 space-y-1.5 text-xs text-gray-600">
            <div className="flex justify-between font-bold text-sm text-gray-800"><span className="font-semibold">Total</span> <span className="text-blue-600">{totalPedidos}</span></div>
            <div className="flex justify-between"><span>Por atender</span> <span>{porAtender}</span></div>
            <div className="flex justify-between"><span>En ruta / Planificados</span> <span>{enRuta}</span></div>
            <div className="flex justify-between"><span>Entregados</span> <span>{entregados}</span></div>
          </div>
        </div>
      </Accordion>

      <Accordion title="Vehículos" id="vehiculos">
        <table className="w-full text-xs text-gray-600 text-center">
          <thead>
            <tr className="border-b border-gray-100">
              <th className="text-left py-1 font-semibold"></th>
              <th className="py-1 font-semibold">Disponible</th>
              <th className="py-1 font-semibold">En ruta</th>
              <th className="py-1 font-semibold">Averiada</th>
              <th className="py-1 font-bold text-gray-800">Total</th>
            </tr>
          </thead>
          <tbody>
            <tr className="border-b border-gray-50">
              <td className="text-left py-1.5"><Truck size={14} className="text-gray-700"/></td>
              <td>{aCount.disp}</td><td>{aCount.ruta}</td><td>{aCount.ave}</td><td className="font-bold text-gray-800">{aCount.tot}</td>
            </tr>
            <tr className="border-b border-gray-50">
              <td className="text-left py-1.5"><span className="text-[14px]">🏍️</span></td>
              <td>{mCount.disp}</td><td>{mCount.ruta}</td><td>{mCount.ave}</td><td className="font-bold text-gray-800">{mCount.tot}</td>
            </tr>
            <tr>
              <td className="text-left py-1.5"><span className="text-[14px]">🚲</span></td>
              <td>{bCount.disp}</td><td>{bCount.ruta}</td><td>{bCount.ave}</td><td className="font-bold text-gray-800">{bCount.tot}</td>
            </tr>
          </tbody>
        </table>
      </Accordion>

      <Accordion title="Stock de almacenes" id="almacenes">
        <div className="space-y-2 text-xs">
          {almacenes.map(alm => (
            <div key={alm.id} className="border border-blue-200 rounded-lg p-2 flex justify-between items-center text-blue-900 font-semibold bg-white">
              <span>{alm.nombre}</span>
              <span>
                {alm.tipo === 'CENTRAL' ? <span className="flex items-center"><span className="text-blue-500 mr-1 text-[16px]">∞</span> Ilimitado</span> : `${alm.stockActual} / ${alm.capacidadMaxima}`}
              </span>
            </div>
          ))}
        </div>
      </Accordion>

      <Accordion title="Incidencias" id="incidencias">
        <div className="space-y-3 text-sm text-gray-700">
          <div className="flex justify-between items-center bg-gray-50 p-2 rounded-lg">
            <span className="flex items-center text-red-600 font-medium"><AlertCircle size={16} className="mr-2" /> Bloqueos ocurridos</span>
            <span className="font-bold">4</span>
          </div>
          <div className="flex justify-between items-center bg-gray-50 p-2 rounded-lg">
            <span className="flex items-center text-gray-600 font-medium"><AlertTriangle size={16} className="mr-2" /> Averías ocurridas</span>
            <span className="font-bold">2</span>
          </div>
        </div>
      </Accordion>
      
      <Accordion title="Costo acumulado" id="costo">
        <div className="text-sm font-semibold text-gray-500 text-center py-4">Calculando...</div>
      </Accordion>

      <Accordion title="Detalles del vehículo" id="detalle_vehiculo">
        {selectedUnidad ? (
          <div className="text-xs text-gray-600 space-y-2">
            <div className="flex items-center bg-blue-100 rounded-lg p-2 mb-3">
              <div className="bg-blue-200 p-2 rounded-md mr-3">
                <Truck size={20} className="text-blue-800" />
              </div>
              <div>
                <div className="font-bold text-gray-900 text-sm">{selectedUnidad.tipoNombre} {selectedUnidad.codigo}</div>
                <div className="text-blue-600 font-semibold">{selectedUnidad.estadoOperativo.replace('_', ' ')}</div>
              </div>
            </div>
            <div className="flex justify-between"><span className="font-medium text-gray-500">Capacidad utilizada</span> <span>{selectedUnidad.porcentajeCarga.toFixed(1)}%</span></div>
          </div>
        ) : (
          <div className="text-xs text-gray-400 text-center py-2">Selecciona un vehículo en el mapa</div>
        )}
      </Accordion>
      
      <Accordion title="Detalles de pedido" id="detalle_pedido">
        <div className="text-xs text-gray-400 text-center py-2">Selecciona un pedido</div>
      </Accordion>

    </div>
  );
};
