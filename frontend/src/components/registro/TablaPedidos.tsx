import React from 'react';
import type { EstadoPedidoRegistro, PedidoRegistro } from '../../types/registro';
import { formatFechaHora, formatUbicacion, formatVencimiento } from './utils';

interface Props {
  pedidos: PedidoRegistro[];
  cargando: boolean;
}

const estiloEstado: Record<EstadoPedidoRegistro, string> = {
  'Por atender': 'bg-amber-50 text-amber-700 ring-amber-200',
  Planificado: 'bg-green-50 text-green-700 ring-green-200',
};

const columnas = [
  'ID Cliente',
  'Cliente',
  'Ubicación',
  'Cantidad P',
  'Fecha de registro',
  'Plazo',
  'Fecha de vencimiento',
  'Estado',
];

export const TablaPedidos: React.FC<Props> = ({ pedidos, cargando }) => (
  <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-x-auto">
    <table className="w-full text-sm text-left">
      <thead className="bg-blue-600 text-white">
        <tr>
          {columnas.map((c) => (
            <th key={c} className="px-4 py-3 font-semibold whitespace-nowrap">
              {c}
            </th>
          ))}
        </tr>
      </thead>
      <tbody className="text-gray-700">
        {cargando && (
          <tr>
            <td colSpan={columnas.length} className="px-4 py-6 text-center text-gray-400">
              Cargando pedidos...
            </td>
          </tr>
        )}
        {!cargando && pedidos.length === 0 && (
          <tr>
            <td colSpan={columnas.length} className="px-4 py-6 text-center text-gray-400">
              Aún no hay pedidos registrados.
            </td>
          </tr>
        )}
        {pedidos.map((p) => (
          <tr key={p.id} className="border-b border-gray-100 odd:bg-white even:bg-blue-50/50">
            <td className="px-4 py-3 whitespace-nowrap">{p.idCliente}</td>
            <td className="px-4 py-3 whitespace-nowrap">{p.cliente}</td>
            <td className="px-4 py-3 whitespace-nowrap">{formatUbicacion(p.ubicacion)}</td>
            <td className="px-4 py-3">{p.cantidad}</td>
            <td className="px-4 py-3 whitespace-nowrap">{formatFechaHora(p.fechaRegistro)}</td>
            <td className="px-4 py-3 whitespace-nowrap">{p.plazoHoras} h</td>
            <td className="px-4 py-3 whitespace-nowrap">
              {formatVencimiento(p.fechaRegistro, p.plazoHoras)}
            </td>
            <td className="px-4 py-3 whitespace-nowrap">
              <span
                className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ${estiloEstado[p.estado]}`}
              >
                {p.estado}
              </span>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  </div>
);
