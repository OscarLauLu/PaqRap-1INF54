import type { NuevoPedidoDatos, PedidoRegistro } from '../types/registro';

const fecha = (dia: number, hora: number, min: number) =>
  new Date(2026, 7, dia, hora, min).toISOString();

// Datos de ejemplo tomados del diseño de la pantalla de Registro
const MOCK: PedidoRegistro[] = [
  { id: 'PED-1', idCliente: 'c9167', cliente: 'C-34', ubicacion: { posX: 45, posY: 43 }, cantidad: 12, fechaRegistro: fecha(11, 13, 31), plazoHoras: 36, tipoEntrega: 'Normal', estado: 'Por atender' },
  { id: 'PED-2', idCliente: 'c8234', cliente: 'C-34', ubicacion: { posX: 32, posY: 18 }, cantidad: 2, fechaRegistro: fecha(11, 14, 10), plazoHoras: 8, tipoEntrega: 'Priorizada', estado: 'Planificado' },
  { id: 'PED-3', idCliente: 'c1042', cliente: 'C-34', ubicacion: { posX: 32, posY: 18 }, cantidad: 6, fechaRegistro: fecha(11, 14, 10), plazoHoras: 36, tipoEntrega: 'Normal', estado: 'Por atender' },
  { id: 'PED-4', idCliente: 'c3781', cliente: 'C-34', ubicacion: { posX: 32, posY: 18 }, cantidad: 3, fechaRegistro: fecha(11, 14, 25), plazoHoras: 8, tipoEntrega: 'Priorizada', estado: 'Por atender' },
  { id: 'PED-5', idCliente: 'c6254', cliente: 'C-34', ubicacion: { posX: 32, posY: 18 }, cantidad: 10, fechaRegistro: fecha(11, 15, 5), plazoHoras: 12, tipoEntrega: 'Normal', estado: 'Por atender' },
];

// Vive a nivel de módulo para que los pedidos no se pierdan al cambiar de pantalla.
let memoria: PedidoRegistro[] = [...MOCK];

/**
 * PASO 1: todo en memoria (datos falsos).
 * PASO 4: reemplazar el cuerpo de estas funciones por llamadas a apiClient
 * (GET y POST /api/pedidos) sin tocar los componentes.
 */
export const pedidosApi = {
  async listar(): Promise<PedidoRegistro[]> {
    return [...memoria];
  },

  async registrar(datos: NuevoPedidoDatos): Promise<PedidoRegistro> {
    const nuevo: PedidoRegistro = {
      id: `PED-${Date.now()}`,
      idCliente: `c${Math.floor(1000 + Math.random() * 9000)}`,
      cliente: datos.cliente,
      ubicacion: datos.ubicacion,
      cantidad: datos.cantidad,
      fechaRegistro: new Date().toISOString(),
      plazoHoras: datos.plazoHoras,
      tipoEntrega: datos.tipoEntrega,
      estado: 'Por atender',
    };
    memoria = [nuevo, ...memoria];
    return nuevo;
  },
};
