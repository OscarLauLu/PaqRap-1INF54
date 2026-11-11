import React, { useEffect, useMemo, useRef, useState } from 'react';
import { ArrowDownUp, ChevronDown, ChevronRight, Package, CheckCircle2, Search, X } from 'lucide-react';
import type { Pedido, RelojSimuladoData, UnidadTransporte } from '../../types';
import { nivelSemaforoUT, type SemaforoUT } from '../../hooks/filtros';
import { VehiculoRealista } from '../mapa/IconosMapa';
import { rutasApi, type RutaApi } from '../../api/rutasApi';
import { parsearFechaLocal } from '../../utils/fechaLocal';
import { formatMinutos } from '../../utils/reportesDiarios';

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
  reloj?: RelojSimuladoData;
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

// Ordenamientos de la lista: ocupación, hora de salida y hora de llegada a su destino (Sí4)
type Orden = 'desc' | 'asc' | 'salidaAsc' | 'salidaDesc' | 'llegadaAsc' | 'llegadaDesc' | 'codigo';
const ORDENES: { valor: Orden; etiqueta: string }[] = [
  { valor: 'desc', etiqueta: 'Ocupación: Mayor a Menor' },
  { valor: 'asc', etiqueta: 'Ocupación: Menor a Mayor' },
  { valor: 'salidaAsc', etiqueta: 'Hora salida: Más temprana (Asc)' },
  { valor: 'salidaDesc', etiqueta: 'Hora salida: Más tardía (Desc)' },
  { valor: 'llegadaAsc', etiqueta: 'Hora llegada: Más temprana (Asc)' },
  { valor: 'llegadaDesc', etiqueta: 'Hora llegada: Más tardía (Desc)' },
  { valor: 'codigo', etiqueta: 'Por Código' },
];

/** "2026-09-01T08:30:00" → "08:30" (la hora tal como la manda el simulador) */
const horaCorta = (iso?: string) => (iso && iso.length >= 16 ? iso.slice(11, 16) : '');

/** Tiempo de una entrega: desde que el vehículo salió del almacén hasta que llegó al cliente */
interface TiempoEntrega {
  pedido: string;
  minutos: number;
  entregada: boolean;
}

export const ListaUnidadesPanel: React.FC<ListaUnidadesPanelProps> = ({
  unidades,
  pedidos,
  rutas = [],
  selectedUnidadCodigo,
  onSelectUnidad,
  filtroSemaforo = 'TODOS',
  onCambiarFiltroSemaforo,
  reloj,
}) => {
  const [orden, setOrden] = useState<Orden>('desc');
  const [expandido, setExpandido] = useState<string | null>(null);
  const itemRefs = useRef<Map<string, HTMLDivElement>>(new Map());

  // Búsqueda de vehículos por texto (código o tipo)
  const [busquedaTexto, setBusquedaTexto] = useState<string>('');

  // Búsqueda por distancia en minutos: destino inmediato (llegada) u origen (salida) (Sí4)
  const [busquedaTipo, setBusquedaTipo] = useState<'destino' | 'origen'>('destino');
  const [filtroMinutos, setFiltroMinutos] = useState<string>('');

  // Rutas con sus horas (salida del almacén y llegada a cada cliente), para ordenar y medir entregas
  const [rutasHorario, setRutasHorario] = useState<RutaApi[]>([]);
  useEffect(() => {
    let vivo = true;
    const cargar = () => rutasApi.listar().then((lista) => { if (vivo) setRutasHorario(lista); });
    cargar();
    const t = setInterval(cargar, 5000);
    return () => { vivo = false; clearInterval(t); };
  }, []);

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

  // Por unidad: horas de salida, llegada a destino, distancias en minutos y tiempos de entrega
  const horarioDeUnidad = useMemo(() => {
    const salida = new Map<string, number>();
    const salidaTexto = new Map<string, string>();
    const llegadaDestino = new Map<string, number>();
    const llegadaDestinoTexto = new Map<string, string>();
    const minutosOrigen = new Map<string, number>();
    const minutosDestino = new Map<string, number>();
    const tiempos = new Map<string, TiempoEntrega[]>();

    const ahoraMs = parsearFechaLocal(reloj?.instanteActual);

    rutasHorario.forEach((r) => {
      const salidaMs = parsearFechaLocal(r.fechaHoraGeneracion);
      if (salidaMs === null) return;

      const actual = salida.get(r.unidadCodigo);

      // Ruta en curso: la que no está completada y sale primero
      if (r.estado !== 'COMPLETADA') {
        if (actual === undefined || salidaMs < actual) {
          salida.set(r.unidadCodigo, salidaMs);
          salidaTexto.set(r.unidadCodigo, horaCorta(r.fechaHoraGeneracion));

          // Distancia en minutos al origen (almacén de salida)
          if (ahoraMs !== null && ahoraMs >= salidaMs) {
            minutosOrigen.set(r.unidadCodigo, Math.round((ahoraMs - salidaMs) / 60000));
          } else {
            minutosOrigen.set(r.unidadCodigo, 0);
          }

          // Destino inmediato: próxima parada pendiente o regreso a almacén
          const paradasPendientes = (r.paradas || []).filter((p) => !p.entregada);
          let proxLlegadaMs: number | null = null;
          let proxLlegadaTxt = '';

          if (paradasPendientes.length > 0) {
            proxLlegadaMs = parsearFechaLocal(paradasPendientes[0].horaEstimadaLlegada);
            proxLlegadaTxt = horaCorta(paradasPendientes[0].horaEstimadaLlegada);
          } else if (r.horaLlegadaAlmacen) {
            proxLlegadaMs = parsearFechaLocal(r.horaLlegadaAlmacen);
            proxLlegadaTxt = horaCorta(r.horaLlegadaAlmacen);
          }

          if (proxLlegadaMs !== null) {
            llegadaDestino.set(r.unidadCodigo, proxLlegadaMs);
            llegadaDestinoTexto.set(r.unidadCodigo, proxLlegadaTxt);
            const refMs = ahoraMs ?? salidaMs;
            minutosDestino.set(r.unidadCodigo, Math.max(0, Math.round((proxLlegadaMs - refMs) / 60000)));
          }
        }
      } else if (actual === undefined) {
        // Fallback para unidades que solo tienen rutas completadas
        salida.set(r.unidadCodigo, salidaMs);
        salidaTexto.set(r.unidadCodigo, horaCorta(r.fechaHoraGeneracion));
      }
      (r.paradas || []).forEach((p) => {
        const llegadaMs = parsearFechaLocal(p.horaEstimadaLlegada);
        if (llegadaMs === null || llegadaMs < salidaMs) return;
        const lista = tiempos.get(r.unidadCodigo) ?? [];
        lista.push({ pedido: p.codigoPedido, minutos: Math.round((llegadaMs - salidaMs) / 60000), entregada: p.entregada });
        tiempos.set(r.unidadCodigo, lista);
      });
    });
    return { salida, salidaTexto, llegadaDestino, llegadaDestinoTexto, minutosOrigen, minutosDestino, tiempos };
  }, [rutasHorario, reloj?.instanteActual]);

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

    // Búsqueda por texto (código, placa o tipo de vehículo)
    if (busquedaTexto.trim() !== '') {
      const q = busquedaTexto.trim().toLowerCase();
      lista = lista.filter(
        (u) =>
          u.codigo.toLowerCase().includes(q) ||
          u.tipoNombre.toLowerCase().includes(q) ||
          (u.placa && u.placa.toLowerCase().includes(q))
      );
    }

    if (filtroSemaforo && filtroSemaforo !== 'TODOS') {
      lista = lista.filter((u) => nivelSemaforoUT(u) === filtroSemaforo);
    }

    // Filtro por distancia en minutos al destino inmediato o al origen (Sí4)
    if (filtroMinutos.trim() !== '') {
      const maxMin = Number(filtroMinutos);
      if (!isNaN(maxMin)) {
        lista = lista.filter((u) => {
          if (busquedaTipo === 'destino') {
            const m = horarioDeUnidad.minutosDestino.get(u.codigo);
            return m !== undefined && m <= maxMin;
          } else {
            const m = horarioDeUnidad.minutosOrigen.get(u.codigo);
            return m !== undefined && m <= maxMin;
          }
        });
      }
    }

    lista.sort((a, b) => {
      if (orden === 'codigo') {
        return a.codigo.localeCompare(b.codigo, undefined, { numeric: true });
      }
      // Ordenar por hora de salida (Ascendente / Descendente) (Sí4)
      if (orden === 'salidaAsc' || orden === 'salidaDesc') {
        const sa = horarioDeUnidad.salida.get(a.codigo);
        const sb = horarioDeUnidad.salida.get(b.codigo);
        if (sa === undefined && sb === undefined) return a.codigo.localeCompare(b.codigo, undefined, { numeric: true });
        if (sa === undefined) return 1;
        if (sb === undefined) return -1;
        return orden === 'salidaAsc' ? sa - sb : sb - sa;
      }
      // Ordenar por hora de llegada a su destino (cliente/almacén) (Ascendente / Descendente) (Sí4)
      if (orden === 'llegadaAsc' || orden === 'llegadaDesc') {
        const la = horarioDeUnidad.llegadaDestino.get(a.codigo);
        const lb = horarioDeUnidad.llegadaDestino.get(b.codigo);
        if (la === undefined && lb === undefined) return a.codigo.localeCompare(b.codigo, undefined, { numeric: true });
        if (la === undefined) return 1;
        if (lb === undefined) return -1;
        return orden === 'llegadaAsc' ? la - lb : lb - la;
      }
      const capA = a.capacidadMaxima || 1;
      const capB = b.capacidadMaxima || 1;
      const pctA = a.cargaActual / capA;
      const pctB = b.cargaActual / capB;
      return orden === 'desc' ? pctB - pctA : pctA - pctB;
    });

    return lista;
  }, [unidades, busquedaTexto, filtroSemaforo, filtroMinutos, busquedaTipo, orden, horarioDeUnidad]);

  return (
    <div className="space-y-3">
      {/* Controles: Búsqueda, Ordenamiento por Hora de Salida, Semáforo y Distancia en minutos */}
      <div className="space-y-2">
        {/* Cabecera y contador */}
        <div className="flex items-center justify-between gap-2 text-xs">
          <span className="font-semibold text-(--color-ink-500)">
            {unidadesFiltradas.length} de {unidades.length} unidades
          </span>
          <span className="text-[11px] font-medium text-brand-600 bg-brand-50 px-2 py-0.5 rounded border border-brand-200 truncate max-w-[190px]">
            {ORDENES.find((o) => o.valor === orden)?.etiqueta}
          </span>
        </div>

        {/* Barra de búsqueda de vehículos */}
        <div className="relative">
          <input
            type="text"
            placeholder="Buscar vehículo (AUTO-01, Moto, Bicicleta...)"
            value={busquedaTexto}
            onChange={(e) => setBusquedaTexto(e.target.value)}
            className="w-full bg-white border border-gray-300 rounded-md pl-8 pr-7 py-1.5 text-xs text-ink-900 placeholder-ink-400 focus:outline-none focus:border-brand-500"
          />
          <Search size={14} className="absolute left-2.5 top-2 text-ink-400" />
          {busquedaTexto && (
            <button
              type="button"
              onClick={() => setBusquedaTexto('')}
              className="absolute right-2 top-2 text-gray-400 hover:text-gray-600"
              title="Limpiar búsqueda"
            >
              <X size={14} />
            </button>
          )}
        </div>

        {/* Capacidad de ordenar por hora de salida y otros criterios */}
        <div className="bg-gray-50/90 border border-gray-200 rounded-lg p-2 space-y-1.5 text-xs">
          <div className="flex items-center justify-between gap-1">
            <label htmlFor="ordenar-unidades" className="text-[11px] font-bold text-ink-700 flex items-center gap-1">
              <ArrowDownUp size={12} className="text-brand-500" />
              <span>Ordenar vehículos:</span>
            </label>
            <select
              id="ordenar-unidades"
              value={orden}
              onChange={(e) => setOrden(e.target.value as Orden)}
              className="bg-white border border-gray-300 rounded px-2 py-1 text-[11px] font-semibold text-ink-900 outline-none flex-1 max-w-[190px] cursor-pointer hover:border-brand-400 truncate"
            >
              <optgroup label="⏰ Hora de salida (Ruta)">
                <option value="salidaAsc">Hora salida: Más temprana (Asc)</option>
                <option value="salidaDesc">Hora salida: Más tardía (Desc)</option>
              </optgroup>
              <optgroup label="🏁 Hora de llegada (Destino)">
                <option value="llegadaAsc">Hora llegada: Más temprana (Asc)</option>
                <option value="llegadaDesc">Hora llegada: Más tardía (Desc)</option>
              </optgroup>
              <optgroup label="📦 Carga y ocupación">
                <option value="desc">Ocupación: Mayor a Menor</option>
                <option value="asc">Ocupación: Menor a Mayor</option>
              </optgroup>
              <optgroup label="🏷️ Código">
                <option value="codigo">Por Código</option>
              </optgroup>
            </select>
          </div>

          {/* Botones directos para ordenar por hora de salida */}
          <div className="flex items-center gap-1.5 pt-0.5 text-[10px]">
            <span className="text-ink-500 font-semibold shrink-0">Hora de salida:</span>
            <button
              type="button"
              onClick={() => setOrden('salidaAsc')}
              className={`flex-1 py-1 px-1.5 rounded border text-center font-bold transition-colors ${
                orden === 'salidaAsc'
                  ? 'bg-brand-500 text-white border-brand-500 shadow-xs'
                  : 'bg-white text-ink-700 border-gray-200 hover:bg-gray-100'
              }`}
              title="Ordenar por hora de salida más temprana"
            >
              &uarr; Más temprana
            </button>
            <button
              type="button"
              onClick={() => setOrden('salidaDesc')}
              className={`flex-1 py-1 px-1.5 rounded border text-center font-bold transition-colors ${
                orden === 'salidaDesc'
                  ? 'bg-brand-500 text-white border-brand-500 shadow-xs'
                  : 'bg-white text-ink-700 border-gray-200 hover:bg-gray-100'
              }`}
              title="Ordenar por hora de salida más tardía"
            >
              &darr; Más tardía
            </button>
          </div>
        </div>

        {/* Búsqueda por distancia en minutos al destino inmediato o al origen (Sí4) */}
        <div className="bg-gray-50/90 border border-gray-200 rounded-lg p-2 space-y-1.5 text-xs">
          <div className="flex items-center justify-between text-[11px] font-bold text-(--color-ink-700)">
            <span>Buscar UT por distancia (minutos)</span>
            <span className="text-[10px] font-semibold text-brand-600">Distinguir llegada / salida</span>
          </div>
          <div className="flex gap-1.5 items-center">
            <select
              value={busquedaTipo}
              onChange={(e) => setBusquedaTipo(e.target.value as 'destino' | 'origen')}
              className="bg-white border border-gray-300 rounded px-1.5 py-1 text-[11px] font-semibold text-ink-900 outline-none flex-1 truncate"
            >
              <option value="destino">Al destino inmediato (llegada)</option>
              <option value="origen">Al origen (salida del almacén)</option>
            </select>
            <div className="flex items-center gap-1 shrink-0">
              <span className="text-[11px] text-gray-500">&le;</span>
              <input
                type="number"
                min={0}
                placeholder="Minutos"
                value={filtroMinutos}
                onChange={(e) => setFiltroMinutos(e.target.value)}
                className="w-16 bg-white border border-gray-300 rounded px-1.5 py-1 text-[11px] text-center font-bold text-ink-900 outline-none"
              />
              <span className="text-[11px] text-gray-500">min</span>
              {filtroMinutos && (
                <button
                  type="button"
                  onClick={() => setFiltroMinutos('')}
                  className="text-gray-400 hover:text-gray-600 px-1 font-bold text-xs"
                  title="Limpiar búsqueda por minutos"
                >
                  &times;
                </button>
              )}
            </div>
          </div>
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

            // Hora de salida y tiempos de entrega de esta unidad
            const salidaMs = horarioDeUnidad.salida.get(u.codigo);
            const tiempos = horarioDeUnidad.tiempos.get(u.codigo) ?? [];
            const entregasHechas = tiempos.filter((t) => t.entregada);

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
                      {/* Hora de salida y llegada a destino inmediato (Sí4) */}
                      <div className="ml-auto flex items-center gap-1.5 text-[10px] text-gray-500 font-medium">
                        {salidaMs !== undefined && (
                          <span title="Hora de salida del almacén">
                            Salida: {horarioDeUnidad.salidaTexto.get(u.codigo)}
                          </span>
                        )}
                        {horarioDeUnidad.llegadaDestinoTexto.get(u.codigo) && (
                          <span title="Hora estimada de llegada a destino inmediato (cliente o almacén)">
                            · Llegada: {horarioDeUnidad.llegadaDestinoTexto.get(u.codigo)}
                          </span>
                        )}
                      </div>
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

                    {/* Distancias en minutos al origen (salida) y destino inmediato (llegada) (Sí4) */}
                    {(horarioDeUnidad.minutosOrigen.has(u.codigo) || horarioDeUnidad.minutosDestino.has(u.codigo)) && (
                      <div className="flex items-center justify-between gap-1 text-[10px] text-gray-500 mt-0.5 pt-0.5 border-t border-gray-100/80">
                        <span title="Distancia en minutos al origen (almacén de donde salió)">
                          🛫 Origen (salida): <b className="text-ink-800">{horarioDeUnidad.minutosOrigen.get(u.codigo) ?? 0} min</b>
                        </span>
                        <span title="Distancia en minutos al destino inmediato (llegada)">
                          🏁 Destino (llegada): <b className="text-brand-600">{horarioDeUnidad.minutosDestino.get(u.codigo) ?? '—'} min</b>
                        </span>
                      </div>
                    )}
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

                    {/* Tiempo que le tomó cada entrega ya realizada */}
                    <div className="pt-1.5 mt-1 border-t border-gray-200">
                      <div className="flex items-center gap-1 text-gray-500 font-semibold mb-1">
                        <CheckCircle2 size={12} className="text-green-600" />
                        Entregas realizadas ({entregasHechas.length})
                      </div>
                      {entregasHechas.length === 0 ? (
                        <p className="text-gray-400 italic py-0.5">Aún no entrega pedidos.</p>
                      ) : (
                        entregasHechas.map((t) => (
                          <div key={t.pedido} className="flex items-center justify-between px-2 py-0.5">
                            <span className="font-semibold text-(--color-ink-900)">{t.pedido}</span>
                            <span className="font-bold text-green-700">{formatMinutos(t.minutos)}</span>
                          </div>
                        ))
                      )}
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
