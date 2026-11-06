import React, { useEffect, useMemo, useRef, useState } from 'react';
import { ArrowDownUp, ChevronDown, ChevronRight, Package, Truck, AlertCircle } from 'lucide-react';
import type { Pedido, UnidadTransporte } from '../../types';
import { nivelSemaforoUT, type SemaforoUT } from '../../hooks/filtros';
import { VehiculoRealista } from '../mapa/IconosMapa';

export interface RutaInfoBasica {
  unidadCodigo: string;
  codigosPedidos?: string[];
}

interface ListaUnidadesPanelProps {
  unidades: UnidadTransporte[];
  pedidos: Pedido[];
  rutas?: RutaInfoBasica[];
  selectedUnidadCodigo: string | null;
  onSelectUnidad: (codigo: string) => void;
  filtroSemaforo?: SemaforoUT;
  onCambiarFiltroSemaforo?: (sem: SemaforoUT) => void;
}

const COLOR_TIPO: Record<string, string> = {
  Auto: '#2563EB',
  Moto: '#9333EA',
  Bicicleta: '#EC4899',
};

const SEMAFORO_CONFIG: Record<
  'VACIO' | 'VERDE' | 'AMBAR' | 'ROJO',
  { etiqueta: string; badgeClase: string; dotColor: string }
> = {
  VACIO: { etiqueta: 'Vacío', badgeClase: 'bg-gray-100 text-gray-700 border-gray-300', dotColor: '#9CA3AF' },
  VERDE: { etiqueta: 'Bajo', badgeClase: 'bg-green-50 text-green-700 border-green-300', dotColor: '#22C55E' },
  AMBAR: { etiqueta: 'Medio', badgeClase: 'bg-amber-50 text-amber-700 border-amber-300', dotColor: '#F59E0B' },
  ROJO: { etiqueta: 'Lleno', badgeClase: 'bg-red-50 text-red-700 border-red-300', dotColor: '#EF4444' },
};

export const ListaUnidadesPanel: React.FC<ListaUnidadesPanelProps> = ({
  unidades,
  pedidos,
  rutas = [],
  selectedUnidadCodigo,
  onSelectUnidad,
  filtroSemaforo = 'TODOS',
  onCambiarFiltroSemaforo,
}) => {
  const [orden, setOrden] = useState<'desc' | 'asc' | 'codigo'>('desc');
  const [expandido, setExpandido] = useState<string | null>(null);
  const itemRefs = useRef<Map<string, HTMLDivElement>>(new Map());

  // Mapeo de pedidos por código
  const pedidosPorCodigo = useMemo(() => {
    const map = new Map<string, Pedido>();
    pedidos.forEach((p) => map.set(p.codigo, p));
    return map;
  }, [pedidos]);

  // Mapeo de códigos de pedidos asignados por unidad
  const pedidosDeUnidad = useMemo(() => {
    const map = new Map<string, string[]>();
    rutas.forEach((r) => {
      if (r.codigosPedidos && r.codigosPedidos.length > 0) {
        map.set(r.unidadCodigo, r.codigosPedidos);
      }
    });
    return map;
  }, [rutas]);

  // Auto-scroll al seleccionar desde el mapa
  useEffect(() => {
    if (selectedUnidadCodigo) {
      const el = itemRefs.current.get(selectedUnidadCodigo);
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      }
    }
  }, [selectedUnidadCodigo]);

  // Filtrado y ordenamiento de unidades
  const unidadesFiltradas = useMemo(() => {
    let lista = [...unidades];

    if (filtroSemaforo && filtroSemaforo !== 'TODOS') {
      lista = lista.filter((u) => nivelSemaforoUT(u) === filtroSemaforo);
    }

    lista.sort((a, b) => {
      if (orden === 'codigo') {
        return a.codigo.localeCompare(b.codigo, undefined, { numeric: true });
      }
      const capA = a.capacidadMaxima || 1;
      const capB = b.capacidadMaxima || 1;
      const pctA = a.cargaActual / capA;
      const pctB = b.cargaActual / capB;
      return orden === 'desc' ? pctB - pctA : pctA - pctB;
    });

    return lista;
  }, [unidades, filtroSemaforo, orden]);

  return (
    <div className="space-y-3">
      {/* Controles: Ordenamiento y Filtro rápido de semáforo */}
      <div className="space-y-2">
        <div className="flex items-center justify-between gap-2 text-xs">
          <span className="font-semibold text-(--color-ink-500)">
            {unidadesFiltradas.length} de {unidades.length} unidades
          </span>

          <button
            type="button"
            onClick={() => setOrden((prev) => (prev === 'desc' ? 'asc' : prev === 'asc' ? 'codigo' : 'desc'))}
            className="flex items-center gap-1 font-semibold text-(--color-brand-500) hover:text-(--color-brand-600) bg-gray-50 border border-gray-200 px-2 py-1 rounded-md transition-colors"
            title="Cambiar orden de la lista"
          >
            <ArrowDownUp size={12} />
            <span>
              {orden === 'desc'
                ? 'Ocupación: Mayor a Menor'
                : orden === 'asc'
                ? 'Ocupación: Menor a Mayor'
                : 'Por Código'}
            </span>
          </button>
        </div>

        {/* Botones de filtro por semáforo */}
        {onCambiarFiltroSemaforo && (
          <div className="flex items-center gap-1">
            {(['TODOS', 'VACIO', 'VERDE', 'AMBAR', 'ROJO'] as const).map((sem) => {
              const activo = filtroSemaforo === sem;
              return (
                <button
                  key={sem}
                  type="button"
                  onClick={() => onCambiarFiltroSemaforo(sem)}
                  className={`flex-1 py-1 rounded text-[11px] font-bold border transition-colors ${
                    activo
                      ? 'bg-(--color-brand-500) text-white border-(--color-brand-500)'
                      : 'bg-white text-(--color-ink-700) border-gray-200 hover:bg-gray-50'
                  }`}
                >
                  {sem === 'TODOS' ? 'Todos' : sem === 'VACIO' ? 'Vacío' : sem === 'VERDE' ? 'Bajo' : sem === 'AMBAR' ? 'Medio' : 'Lleno'}
                </button>
              );
            })}
          </div>
        )}
      </div>

      {/* Lista de Unidades */}
      <div className="space-y-1.5 max-h-72 overflow-y-auto pr-1 -mr-1 scrollbar-thin">
        {unidadesFiltradas.length === 0 ? (
          <p className="text-center text-xs text-gray-400 py-3">No hay unidades con este filtro.</p>
        ) : (
          unidadesFiltradas.map((u) => {
            const cap = u.capacidadMaxima || 1;
            const pct = Math.min(100, Math.round((u.cargaActual / cap) * 100));
            const sem = nivelSemaforoUT(u);
            const semCfg = SEMAFORO_CONFIG[sem];
            const esSeleccionado = selectedUnidadCodigo === u.codigo;
            const estaExpandido = expandido === u.codigo;

            // Envíos / pedidos asignados a esta unidad
            const codigosAsignados = pedidosDeUnidad.get(u.codigo) ?? (u.pedidoAsignado ? [u.pedidoAsignado] : []);
            const listaPedidos = codigosAsignados
              .map((cod) => pedidosPorCodigo.get(cod))
              .filter((p): p is Pedido => p !== undefined);

            const colorVehiculo = COLOR_TIPO[u.tipoNombre] ?? '#64748B';

            return (
              <div
                key={u.codigo}
                ref={(el) => {
                  if (el) itemRefs.current.set(u.codigo, el);
                  else itemRefs.current.delete(u.codigo);
                }}
                className={`rounded-lg border transition-all text-xs ${
                  esSeleccionado
                    ? 'border-(--color-brand-500) bg-(--color-brand-50)/70 shadow-xs ring-1 ring-(--color-brand-500)'
                    : 'border-gray-200 bg-white hover:border-gray-300'
                }`}
              >
                {/* Cabecera del ítem: clic selecciona la unidad y resalta en el mapa */}
                <div
                  className="p-2 flex items-center gap-2 cursor-pointer select-none"
                  onClick={() => onSelectUnidad(u.codigo)}
                >
                  {/* Ícono miniatura del vehículo */}
                  <svg viewBox="-20 -20 40 40" className="w-5 h-5 shrink-0" aria-hidden="true">
                    <VehiculoRealista tipo={u.tipoNombre} color={colorVehiculo} />
                  </svg>

                  {/* Código y tipo */}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-1.5">
                      <span className="font-bold text-(--color-ink-900)">{u.codigo}</span>
                      <span className="text-[10px] text-gray-400 font-medium">({u.tipoNombre})</span>
                      {u.estadoOperativo === 'AVERIADA' && (
                        <span className="text-[9px] font-bold text-red-600 bg-red-100 px-1 py-0.2 rounded">Averiada</span>
                      )}
                    </div>

                    {/* Barra de ocupación / stock */}
                    <div className="flex items-center gap-2 mt-1">
                      <div className="flex-1 bg-gray-100 rounded-full h-1.5 overflow-hidden">
                        <div
                          className="h-1.5 rounded-full transition-all"
                          style={{
                            width: `${pct}%`,
                            backgroundColor: semCfg.dotColor,
                          }}
                        />
                      </div>
                      <span className="text-[10px] font-bold text-(--color-ink-700) w-12 text-right">
                        {u.cargaActual}/{cap} u
                      </span>
                    </div>
                  </div>

                  {/* Badge semáforo de ocupación */}
                  <span
                    className={`inline-flex items-center gap-1 px-1.5 py-0.5 rounded border text-[10px] font-bold shrink-0 ${semCfg.badgeClase}`}
                  >
                    <span className="w-1.5 h-1.5 rounded-full" style={{ backgroundColor: semCfg.dotColor }} />
                    {pct}%
                  </span>

                  {/* Botón para expandir envíos */}
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      setExpandido(estaExpandido ? null : u.codigo);
                    }}
                    className="p-1 text-gray-400 hover:text-gray-700 rounded transition-colors"
                    title={estaExpandido ? 'Ocultar pedidos trasladados' : 'Ver pedidos trasladados'}
                    aria-label={estaExpandido ? 'Ocultar pedidos' : 'Ver pedidos'}
                  >
                    {estaExpandido ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
                  </button>
                </div>

                {/* Acordeón: Detalle de envíos y productos (paquetes) que traslada */}
                {estaExpandido && (
                  <div className="border-t border-gray-100 bg-gray-50/70 p-2.5 space-y-1.5 text-[11px]">
                    <div className="flex items-center justify-between text-gray-500 font-semibold mb-1">
                      <span className="flex items-center gap-1">
                        <Package size={12} className="text-(--color-brand-500)" />
                        Envíos trasladados ({listaPedidos.length})
                      </span>
                      <span>Total: {u.cargaActual} paquetes</span>
                    </div>

                    {listaPedidos.length === 0 ? (
                      <p className="text-gray-400 italic py-1">Sin envíos asignados en este instante.</p>
                    ) : (
                      listaPedidos.map((ped) => (
                        <div
                          key={ped.codigo}
                          className="flex items-center justify-between bg-white border border-gray-200 rounded px-2 py-1"
                        >
                          <div>
                            <span className="font-bold text-(--color-ink-900) mr-1.5">{ped.codigo}</span>
                            <span className="text-gray-400 text-[10px]">
                              Destino ({ped.destino.posX}, {ped.destino.posY})
                            </span>
                          </div>
                          <span className="font-semibold text-(--color-brand-500)">
                            {ped.cantidad} {ped.cantidad === 1 ? 'paquete' : 'paquetes'}
                          </span>
                        </div>
                      ))
                    )}
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
