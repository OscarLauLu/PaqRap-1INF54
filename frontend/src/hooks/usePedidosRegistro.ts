import { useCallback, useEffect, useState } from 'react';
import { pedidosApi } from '../api/pedidosApi';
import type { NuevoPedidoDatos, PedidoRegistro } from '../types/registro';

export const usePedidosRegistro = () => {
  const [pedidos, setPedidos] = useState<PedidoRegistro[]>([]);
  const [cargando, setCargando] = useState(true);

  useEffect(() => {
    pedidosApi
      .listar()
      .then(setPedidos)
      .finally(() => setCargando(false));
  }, []);

  const registrar = useCallback(async (datos: NuevoPedidoDatos) => {
    const nuevo = await pedidosApi.registrar(datos);
    setPedidos((prev) => [nuevo, ...prev]);
    return nuevo;
  }, []);

  return { pedidos, cargando, registrar };
};
