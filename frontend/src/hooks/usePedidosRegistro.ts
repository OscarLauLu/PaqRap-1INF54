import { useCallback, useEffect, useState } from 'react';
import { pedidosApi } from '../api/pedidosApi';
import type { NuevoPedidoDatos, PedidoRegistro } from '../types/registro';

export const usePedidosRegistro = () => {
  const [pedidos, setPedidos] = useState<PedidoRegistro[]>([]);
  const [cargando, setCargando] = useState(true);

  const recargar = useCallback(() => {
    setCargando(true);
    pedidosApi
      .listar()
      .then(setPedidos)
      .finally(() => setCargando(false));
  }, []);

  useEffect(() => {
    recargar();
  }, [recargar]);

  const registrar = useCallback(async (datos: NuevoPedidoDatos) => {
    const nuevo = await pedidosApi.registrar(datos);
    setPedidos((prev) => [nuevo, ...prev]);
    return nuevo;
  }, []);

  return { pedidos, cargando, registrar, recargar };
};
