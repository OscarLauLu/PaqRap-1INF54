import React, { useState } from 'react';
import { Bike, Calendar, Car, ChevronDown, ChevronUp, Clock, Minus, Wrench } from 'lucide-react';
import type { Almacen, Pedido, RelojSimuladoData, UnidadTransporte } from '../../types';

interface Props {
  reloj: RelojSimuladoData;
  almacenes: Almacen[];
  unidades: UnidadTransporte[];
  pedidos: Pedido[];
  totalBloqueos: number;
  totalAverias: number;
  /** true = otra simulación usa el motor: no se muestran datos en vivo. */
  sinDatos?: boolean;
}

const Seccion: React.FC<{ titulo: string; abiertaInicial?: boolean; children: React.ReactNode }> = ({
  titulo,
  abiertaInicial = false,
  children,
}) => {
  const [abierta, setAbierta] = useState(abiertaInicial);
  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
      <button
        type="button"
        onClick={() => setAbierta((v) => !v)}
        className="w-full px-4 py-3 flex justify-between items-center text-gray-500 font-semibold hover:bg-gray-50/50"
      >
        <span className="text-sm mx-auto">{titulo}</span>
        {abierta ? <ChevronUp size={18} className="text-blue-300" /> : <ChevronDown size={18} className="text-blue-300" />}
      </button>
      {abierta && <div className="px-4 pb-4">{children}</div>}
    </div>
  );
};

const contar = (lista: UnidadTransporte[]) => ({
  disponible: lista.filter((u) => u.estadoOperativo === 'DISPONIBLE').length,
  enRuta: lista.filter((u) => u.estadoOperativo === 'EN_RUTA').length,
  averiada: lista.filter((u) => u.estadoOperativo === 'AVERIADO' || u.estadoOperativo === 'EN_MANTENIMIENTO').length,
  total: lista.length,
});

export const PanelOperacion: React.FC<Props> = ({ reloj, almacenes, unidades, pedidos, totalBloqueos, totalAverias, sinDatos = false }) => {
  const v = (n: number | string) => (sinDatos ? '—' : n);
  const total = pedidos.length;
  const porEstado = (estado: string) => pedidos.filter((p) => p.estado === estado).length;
  const entregados = porEstado('ENTREGADO');
  const cumplimiento = !sinDatos && total > 0 ? Math.round((entregados / total) * 100) : 0;

  const filasResumen: Array<[string, number]> = [
    ['Por atender', porEstado('REGISTRADO')],
    ['Planificados', porEstado('PLANIFICADO')],
    ['En ruta', porEstado('EN_RUTA')],
    ['Entregados', entregados],
    ['Reasignados', porEstado('REASIGNADO')],
  ];

  const filasVehiculos = [
    { nombre: 'Auto', Icono: Car, color: 'text-blue-500', datos: contar(unidades.filter((u) => u.tipoNombre === 'Auto')) },
    { nombre: 'Moto', Icono: Bike, color: 'text-green-600', datos: contar(unidades.filter((u) => u.tipoNombre === 'Moto')) },
    { nombre: 'Bicicleta', Icono: Bike, color: 'text-yellow-500', datos: contar(unidades.filter((u) => u.tipoNombre === 'Bicicleta')) },
  ];

  const fechaActual = !sinDatos && reloj.instanteActual
    ? new Date(reloj.instanteActual).toLocaleString('es-PE', {
        day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
      })
    : '--';

  return (
    <div className="w-80 shrink-0 space-y-3">
      {/* Fecha actual */}
      <div className="bg-blue-200/80 rounded-xl border border-blue-300 p-3 text-center text-blue-900">
        <div className="flex items-center justify-center font-bold">
          <Calendar size={16} className="mr-2" /> Fecha actual
        </div>
        <div className="text-sm">{fechaActual}</div>
      </div>

      <Seccion titulo="Resumen de pedidos" abiertaInicial>
        <div className="flex items-center gap-2">
          <Clock size={20} className="text-gray-700" />
          <span className="font-bold text-gray-800 text-lg">Cumplimiento</span>
          <span className="ml-auto font-bold text-blue-600">{v(cumplimiento)} %</span>
        </div>
        <div className="w-full bg-gray-200 rounded-full h-2 mt-2">
          <div className="bg-blue-600 h-2 rounded-full" style={{ width: `${cumplimiento}%` }} />
        </div>
        <div className="pt-3 space-y-1.5 text-xs text-gray-500">
          <div className="flex justify-between font-bold text-gray-800">
            <span>Total</span>
            <span className="text-blue-600">{v(total)}</span>
          </div>
          {filasResumen.map(([etiqueta, n]) => (
            <div key={etiqueta} className="flex justify-between">
              <span>{etiqueta}</span>
              <span>{v(n)}</span>
            </div>
          ))}
        </div>
      </Seccion>

      <Seccion titulo="Vehículos" abiertaInicial>
        <table className="w-full text-xs text-gray-500 text-center">
          <thead>
            <tr>
              <th />
              <th className="py-1 font-medium">Disponible</th>
              <th className="py-1 font-medium">En ruta</th>
              <th className="py-1 font-medium">Averiada</th>
              <th className="py-1 font-bold text-gray-800">Total</th>
            </tr>
          </thead>
          <tbody>
            {filasVehiculos.map(({ nombre, Icono, color, datos }) => (
              <tr key={nombre} className="even:bg-gray-50">
                <td className="text-left py-2 pl-1" title={nombre}>
                  <Icono size={18} className={color} />
                </td>
                <td>{v(datos.disponible)}</td>
                <td>{v(datos.enRuta)}</td>
                <td>{v(datos.averiada)}</td>
                <td className="font-bold text-gray-800">{v(datos.total)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Seccion>

      <Seccion titulo="Stock de almacenes">
        <div className="space-y-2 text-xs">
          {almacenes.map((alm) => (
            <div key={alm.id} className="border border-blue-300 rounded-lg px-3 py-2 flex justify-between items-center text-gray-500 font-medium">
              <span>{alm.nombre}</span>
              {alm.tipo === 'CENTRAL' ? (
                <span className="text-blue-600 font-semibold"><span className="text-base mr-1">∞</span>Ilimitado</span>
              ) : (
                <span className="text-gray-800 font-semibold">{sinDatos ? '—' : `${alm.stockActual} / ${alm.capacidadMaxima ?? '—'}`}</span>
              )}
            </div>
          ))}
        </div>
      </Seccion>

      <Seccion titulo="Incidencias">
        <div className="space-y-3 text-sm text-gray-600">
          <div className="flex items-center justify-between">
            <span className="flex items-center gap-2">
              <span className="w-6 h-6 rounded-full bg-red-500 text-white flex items-center justify-center"><Minus size={14} /></span>
              Bloqueos ocurridos
            </span>
            <span className="font-bold text-gray-800">{v(totalBloqueos)}</span>
          </div>
          <div className="flex items-center justify-between">
            <span className="flex items-center gap-2">
              <span className="w-6 h-6 rounded-full bg-gray-600 text-white flex items-center justify-center"><Wrench size={13} /></span>
              Averías ocurridas
            </span>
            <span className="font-bold text-gray-800">{v(totalAverias)}</span>
          </div>
        </div>
      </Seccion>

      <Seccion titulo="Costo acumulado">
        <div className="space-y-2 text-sm text-gray-600">
          {[
            { nombre: 'Auto', Icono: Car },
            { nombre: 'Moto', Icono: Bike },
            { nombre: 'Bicicleta', Icono: Bike },
          ].map(({ nombre, Icono }) => (
            <div key={nombre} className="flex items-center justify-between">
              <span className="flex items-center gap-2">
                <span className="w-7 h-7 rounded-md bg-indigo-100 flex items-center justify-center"><Icono size={16} className="text-gray-800" /></span>
                {nombre}
              </span>
              <span className="text-gray-400">S/ —</span>
            </div>
          ))}
          <p className="text-[11px] text-gray-400 pt-1">Pendiente: el backend aún no expone el costo acumulado.</p>
        </div>
      </Seccion>
    </div>
  );
};
