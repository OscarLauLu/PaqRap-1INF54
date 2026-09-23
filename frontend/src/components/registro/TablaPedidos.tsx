import React from 'react';
import type { PedidoRegistro } from '../../types/registro';
import { BadgeEstadoPedido } from '../ui/Badge';
import { formatFechaHora, formatUbicacion } from './utils';

interface Props {
  pedidos: PedidoRegistro[];
  cargando: boolean;
}

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
  <div className="bg-white rounded-xl border border-(--color-line-200) shadow-sm overflow-x-auto">
    <table className="w-full text-sm text-left">
      <thead className="bg-(--color-brand-500) text-white">
        <tr>
          {columnas.map((c) => (
            <th key={c} className="px-4 py-3 font-semibold whitespace-nowrap">
              {c}
            </th>
          ))}
        </tr>
      </thead>
      <tbody className="text-(--color-ink-700)">
        {cargando && (
          <tr>
            <td colSpan={columnas.length} className="px-4 py-6 text-center text-(--color-ink-400)">
              Cargando pedidos...
            </td>
          </tr>
        )}
        {!cargando && pedidos.length === 0 && (
          <tr>
            <td colSpan={columnas.length} className="px-4 py-6 text-center text-(--color-ink-400)">
              Aún no hay pedidos registrados.
            </td>
          </tr>
        )}
        {pedidos.map((p) => (
          <tr key={p.id} className="border-b border-gray-100 odd:bg-white even:bg-(--color-brand-50)/50">
            <td className="px-4 py-3 whitespace-nowrap">{p.idCliente}</td>
            <td className="px-4 py-3 whitespace-nowrap">{p.cliente}</td>
            <td className="px-4 py-3 whitespace-nowrap">{formatUbicacion(p.ubicacion)}</td>
            <td className="px-4 py-3">{p.cantidad}</td>
            <td className="px-4 py-3 whitespace-nowrap">{formatFechaHora(p.fechaRegistro)}</td>
            <td className="px-4 py-3 whitespace-nowrap">{p.plazoHoras} h</td>
            <td className="px-4 py-3 whitespace-nowrap">
              {p.plazoLimiteEntrega ? formatFechaHora(p.plazoLimiteEntrega) : '—'}
            </td>
            <td className="px-4 py-3 whitespace-nowrap">
              <BadgeEstadoPedido estado={p.estado} />
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  </div>
);
