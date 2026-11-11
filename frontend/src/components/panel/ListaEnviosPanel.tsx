import React, { useEffect, useMemo, useState } from 'react';
import { Box, ChevronDown, ChevronRight, MapPin, Package, Search, Truck } from 'lucide-react';
import type { Pedido } from '../../types';
import { rutasApi, type RutaApi } from '../../api/rutasApi';

interface ListaEnviosPanelProps {
  pedidos: Pedido[];
  rutas?: RutaApi[];
  selectedPedidoCodigo?: string | null;
  selectedProductoId?: string | null;
  onSelectEnvio: (codigoPedido: string) => void;
  onSelectProducto: (codigoPedido: string, productoId: string, unidadCodigo?: string) => void;
}

const horaCorta = (iso?: string | null) => (iso && iso.length >= 16 ? iso.slice(11, 16) : '—');

export const ListaEnviosPanel: React.FC<ListaEnviosPanelProps> = ({
  pedidos,
  rutas: rutasProp,
  selectedPedidoCodigo,
  selectedProductoId,
  onSelectEnvio,
  onSelectProducto,
}) => {
  const [rutasLocal, setRutasLocal] = useState<RutaApi[]>([]);
  const [busqueda, setBusqueda] = useState('');
  const [expandido, setExpandido] = useState<string | null>(null);

  useEffect(() => {
    if (rutasProp && rutasProp.length > 0) return;
    let vivo = true;
    const cargar = () => rutasApi.listar().then((lista) => { if (vivo) setRutasLocal(lista); });
    cargar();
    const t = setInterval(cargar, 5000);
    return () => { vivo = false; clearInterval(t); };
  }, [rutasProp]);

  const rutas = rutasProp && rutasProp.length > 0 ? rutasProp : rutasLocal;

  // Mapeo: por cada código de pedido, qué rutas y paradas lo trasladan
  const asignacionesPorPedido = useMemo(() => {
    const mapa = new Map<
      string,
      Array<{
        rutaCodigo: string;
        unidadCodigo: string;
        horaSalida: string;
        horaLlegada: string;
        entregada: boolean;
      }>
    >();

    rutas.forEach((r) => {
      (r.paradas || []).forEach((p) => {
        if (!p.codigoPedido) return;
        const lista = mapa.get(p.codigoPedido) ?? [];
        lista.push({
          rutaCodigo: r.codigo,
          unidadCodigo: r.unidadCodigo,
          horaSalida: horaCorta(r.fechaHoraGeneracion),
          horaLlegada: horaCorta(p.horaEstimadaLlegada),
          entregada: p.entregada,
        });
        mapa.set(p.codigoPedido, lista);
      });
    });

    return mapa;
  }, [rutas]);

  const pedidosFiltrados = useMemo(() => {
    let lista = [...pedidos];
    if (busqueda.trim()) {
      const q = busqueda.trim().toLowerCase();
      lista = lista.filter((p) => p.codigo.toLowerCase().includes(q) || p.cliente.toLowerCase().includes(q));
    }
    return lista;
  }, [pedidos, busqueda]);

  return (
    <div className="space-y-2.5 text-xs">
      {/* Buscador de envíos */}
      <div className="relative">
        <input
          type="text"
          placeholder="Buscar envío o cliente..."
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
          className="w-full bg-gray-50 border border-gray-200 rounded-lg pl-7 pr-3 py-1.5 text-xs text-ink-900 outline-none focus:border-brand-500 focus:bg-white transition-colors"
        />
        <Search className="w-3.5 h-3.5 text-gray-400 absolute left-2 top-1/2 -translate-y-1/2" />
        {busqueda && (
          <button
            type="button"
            onClick={() => setBusqueda('')}
            className="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 font-bold"
          >
            &times;
          </button>
        )}
      </div>

      <div className="flex justify-between items-center text-[11px] text-gray-500 px-0.5">
        <span>{pedidosFiltrados.length} envíos para transportar</span>
        <span className="font-semibold text-brand-600">Clic para resaltar en mapa</span>
      </div>

      {/* Lista de envíos */}
      <div className="space-y-1.5 max-h-72 overflow-y-auto pr-1 -mr-1 scrollbar-thin">
        {pedidosFiltrados.length === 0 ? (
          <p className="text-center text-gray-400 py-3 italic">No hay envíos que coincidan.</p>
        ) : (
          pedidosFiltrados.map((ped) => {
            const esSeleccionado = selectedPedidoCodigo === ped.codigo;
            const estaExpandido = expandido === ped.codigo;
            const rutasAsignadas = asignacionesPorPedido.get(ped.codigo) ?? [];

            // Generar identificadores de productos para este envío: PED-XXXX-P1, PED-XXXX-P2, etc.
            const cantProductos = Math.max(1, ped.cantidad || 1);
            const productosIds = Array.from({ length: cantProductos }, (_, i) => `${ped.codigo}-P${i + 1}`);

            return (
              <div
                key={ped.codigo}
                className={`rounded-lg border transition-all ${
                  esSeleccionado
                    ? 'border-brand-500 bg-brand-50/70 shadow-xs ring-1 ring-brand-500'
                    : 'border-gray-200 bg-white hover:border-gray-300'
                }`}
              >
                {/* Cabecera del envío: clic resalta el envío completo y sus rutas en el mapa (Sí4) */}
                <div
                  className="p-2 flex items-center gap-2 cursor-pointer select-none"
                  onClick={() => onSelectEnvio(ped.codigo)}
                  title="Clic para resaltar el envío y todas sus rutas en el mapa"
                >
                  <Package className={`w-4 h-4 shrink-0 ${esSeleccionado ? 'text-brand-600' : 'text-gray-500'}`} />

                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between">
                      <span className="font-bold text-ink-900">{ped.codigo}</span>
                      <span className="text-[10px] text-gray-400 font-medium truncate max-w-[100px]" title={ped.cliente}>
                        {ped.cliente}
                      </span>
                    </div>

                    <div className="flex items-center gap-2 text-[10px] text-gray-500 mt-0.5">
                      <span className="flex items-center gap-0.5">
                        <MapPin size={10} className="text-brand-500 shrink-0" />
                        ({ped.destino.posX}, {ped.destino.posY})
                      </span>
                      <span>·</span>
                      <span className="font-semibold text-brand-600">{cantProductos} {cantProductos === 1 ? 'producto' : 'productos'}</span>
                    </div>
                  </div>

                  {/* Botón acordeón para ver productos y rutas */}
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      setExpandido(estaExpandido ? null : ped.codigo);
                    }}
                    className="p-1 text-gray-400 hover:text-gray-700 rounded transition-colors"
                    title={estaExpandido ? 'Ocultar detalle de productos' : 'Ver productos y rutas'}
                    aria-label="Ver productos y rutas"
                  >
                    {estaExpandido ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
                  </button>
                </div>

                {/* Acordeón: Detalle de Rutas, UTs y Productos individuales (Sí4) */}
                {estaExpandido && (
                  <div className="border-t border-gray-100 bg-gray-50/80 p-2.5 space-y-2 text-[11px]">
                    {/* Sección 1: Rutas y UTs asignadas al envío (muestra todas si está dividido) */}
                    <div>
                      <span className="block text-[10px] font-bold text-gray-500 uppercase tracking-wide mb-1">
                        {rutasAsignadas.length > 1
                          ? `Transportado en ${rutasAsignadas.length} unidades de transporte:`
                          : 'Ruta y Unidad asignada:'}
                      </span>

                      {rutasAsignadas.length === 0 ? (
                        <p className="text-gray-400 italic py-0.5">Pendiente de planificación / en almacén</p>
                      ) : (
                        rutasAsignadas.map((asig, idx) => (
                          <div
                            key={`${asig.rutaCodigo}-${asig.unidadCodigo}-${idx}`}
                            className="flex items-center justify-between bg-white border border-gray-200 rounded px-2 py-1 mb-1"
                          >
                            <div className="flex items-center gap-1.5">
                              <Truck size={12} className="text-brand-500" />
                              <span className="font-bold text-ink-900">{asig.unidadCodigo}</span>
                              <span className="text-[10px] text-gray-400">({asig.rutaCodigo})</span>
                            </div>
                            <div className="flex items-center gap-2 text-[10px] text-gray-600">
                              <span title="Hora de salida">Salida: {asig.horaSalida}</span>
                              <span>&rarr;</span>
                              <span title="Hora estimada de llegada">Llegada: {asig.horaLlegada}</span>
                            </div>
                          </div>
                        ))
                      )}
                    </div>

                    {/* Sección 2: Identificadores de producto (Sí4) */}
                    <div>
                      <span className="block text-[10px] font-bold text-gray-500 uppercase tracking-wide mb-1">
                        Identificadores de Producto ({productosIds.length}):
                      </span>
                      <div className="flex flex-wrap gap-1">
                        {productosIds.map((prodId, idx) => {
                          const esProdSel = selectedProductoId === prodId;
                          // Asignar a la ruta correspondiente si hay varias
                          const rutaAsociada = rutasAsignadas[idx % Math.max(1, rutasAsignadas.length)];
                          return (
                            <button
                              key={prodId}
                              type="button"
                              onClick={(e) => {
                                e.stopPropagation();
                                onSelectProducto(ped.codigo, prodId, rutaAsociada?.unidadCodigo);
                              }}
                              className={`flex items-center gap-1 px-1.5 py-0.5 rounded border text-[10px] font-mono transition-colors ${
                                esProdSel
                                  ? 'bg-brand-500 text-white border-brand-600 shadow-xs'
                                  : 'bg-white text-ink-700 border-gray-300 hover:border-brand-400 hover:bg-brand-50/50'
                              }`}
                              title={`Clic para resaltar la ruta del producto ${prodId} en el mapa`}
                            >
                              <Box size={10} />
                              <span>{prodId}</span>
                            </button>
                          );
                        })}
                      </div>
                      <span className="block text-[9px] text-gray-400 mt-1 italic">
                        Haz clic en un producto para resaltar su ruta en el mapa.
                      </span>
                    </div>
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
