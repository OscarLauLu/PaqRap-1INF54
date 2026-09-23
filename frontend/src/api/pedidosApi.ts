import { apiClient } from './client';
import type { NuevoPedidoDatos, PedidoRegistro } from '../types/registro';
import { tipoEntregaEnum } from '../types/registro';

// Forma real que devuelve el backend: PedidoDTO.java (pedidos/dto/PedidoDTO.java).
interface PedidoDTOBackend {
  id: number;
  codigo: string;
  clienteId: string;
  nombreCliente: string;
  destino: { posX: number; posY: number };
  cantidadUnidades: number;
  tipoEntrega: string; // p.ej. PRIORIZADA_8H, REGULAR_36H
  estado: PedidoRegistro['estado'];
  nivelCriticidad: string;
  fechaHoraRegistro: string;
  plazoLimiteEntrega: string;
  fechaHoraEntrega: string | null;
  holguraHoras: number;
  unidadAsignadaId: string | null;
  rutaAsignadaId: number | null;
}

const horasDesdeTipoEntrega = (tipoEntrega: string): number => {
  const match = tipoEntrega.match(/(\d+)H$/);
  return match ? Number(match[1]) : 36;
};

const mapearPedido = (dto: PedidoDTOBackend): PedidoRegistro => ({
  id: dto.id,
  codigo: dto.codigo,
  idCliente: dto.clienteId,
  cliente: dto.nombreCliente,
  ubicacion: dto.destino,
  cantidad: dto.cantidadUnidades,
  fechaRegistro: dto.fechaHoraRegistro,
  plazoHoras: horasDesdeTipoEntrega(dto.tipoEntrega),
  tipoEntregaEnum: dto.tipoEntrega,
  estado: dto.estado,
  plazoLimiteEntrega: dto.plazoLimiteEntrega,
});

export const pedidosApi = {
  async listar(): Promise<PedidoRegistro[]> {
    const response = await apiClient.get<PedidoDTOBackend[]>('/api/pedidos');
    return response.data.map(mapearPedido);
  },

  async registrar(datos: NuevoPedidoDatos): Promise<PedidoRegistro> {
    // El formulario pide un único campo "Cliente"; se usa como idCliente (clave natural) y como
    // nombre para mostrar, ya que el diseño de Figma no distingue ambos campos.
    const response = await apiClient.post<PedidoDTOBackend>('/api/pedidos', {
      idCliente: datos.cliente.trim(),
      nombreCliente: datos.cliente.trim(),
      destinoX: datos.ubicacion.posX,
      destinoY: datos.ubicacion.posY,
      cantidadUnidades: datos.cantidad,
      tipoEntrega: tipoEntregaEnum(datos.tipoEntrega, datos.plazoHoras),
    });
    return mapearPedido(response.data);
  },
};
