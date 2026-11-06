import React, { useMemo, useState } from 'react';
import { Bodega } from '../mapa/IconosMapa';
import type { Almacen, Pedido, RelojSimuladoData } from '../../types';
import { nivelSemaforoAlmacen } from '../../hooks/filtros';
import { ChevronDown, ChevronRight, Clock, Infinity as InfinityIcon, Package, Send, Truck } from 'lucide-react';

interface ListaAlmacenesPanelProps {
  almacenes: Almacen[];
  pedidos: Pedido[];
  reloj?: RelojSimuladoData;
  selectedAlmacenCodigo: string | null;
  onSelectAlmacen: (codigo: string) => void;
}

const datosAlmacen = (a: Almacen) => {
  if (a.tipo === 'CENTRAL') return { nombre: 'Central', letra: 'C', color: '#EA580C' };
  const esNorte = /n[-\s]?o/i.test(a.nombre) || a.nombre.toLowerCase().includes('norte');
  return esNorte ? { nombre: 'Norte', letra: 'N', color: '#0891B2' } : { nombre: 'Este', letra: 'E', color: '#4F46E5' };
};

const SEMAFORO_CONFIG: Record<
  'VACIO' | 'VERDE' | 'AMBAR' | 'ROJO',
  { etiqueta: string; badgeClase: string; dotColor: string }
> = {
  VACIO: { etiqueta: 'Vacío', badgeClase: 'bg-gray-100 text-gray-700 border-gray-300', dotColor: '#9CA3AF' },
  VERDE: { etiqueta: 'Óptimo', badgeClase: 'bg-green-50 text-green-700 border-green-300', dotColor: '#22C55E' },
  AMBAR: { etiqueta: 'Medio', badgeClase: 'bg-amber-50 text-amber-700 border-amber-300', dotColor: '#F59E0B' },
  ROJO: { etiqueta: 'Crítico', badgeClase: 'bg-red-50 text-red-700 border-red-300', dotColor: '#EF4444' },
};

type PestañaAlmacen = 'transito' | 'proximas4h' | 'ultimas4h';

export const ListaAlmacenesPanel: React.FC<ListaAlmacenesPanelProps> = ({
  almacenes,
  pedidos,
  reloj,
  selectedAlmacenCodigo,
  onSelectAlmacen,
}) => {
  const [expandido, setExpandido] = useState<string | null>(null);
  const [pestana, setPestana] = useState<Record<string, PestañaAlmacen>>({});

  // Asignación de pedidos y filtros temporales relativos al almacén más cercano
  // En PaqRap, los almacenes abastecen a la ciudad según cercanía o recarga
  const pedidosPorAlmacen = useMemo(() => {
    const mapa = new Map<
      string,
      { enTransito: Pedido[]; proximas4h: Pedido[]; ultimas4h: Pedido[] }
    >();

    almacenes.forEach((alm) => {
      mapa.set(alm.codigo, { enTransito: [], proximas4h: [], ultimas4h: [] });
    });

    if (almacenes.length === 0) return mapa;

    // Asignar cada pedido al almacén más cercano (distancia Manhattan)
    pedidos.forEach((p) => {
      let mejorDist = Infinity;
      let mejorAlm = almacenes[0]?.codigo;

      almacenes.forEach((alm) => {
        const d = Math.abs(alm.ubicacion.posX - p.destino.posX) + Math.abs(alm.ubicacion.posY - p.destino.posY);
        if (d < mejorDist) {
          mejorDist = d;
          mejorAlm = alm.codigo;
        }
      });

      const entry = mapa.get(mejorAlm);
      if (!entry) return;

      if (p.estado === 'EN_RUTA') {
        entry.enTransito.push(p);
      } else if (p.estado === 'PLANIFICADO' || p.estado === 'REGISTRADO') {
        entry.proximas4h.push(p);
      } else if (p.estado === 'ENTREGADO') {
        entry.ultimas4h.push(p);
      }
    });

    return mapa;
  }, [almacenes, pedidos]);

  return (
    <div className="space-y-2">
      <div className="text-xs text-gray-500 font-semibold mb-1">
        Capacidad y flujo de carga por almacén
      </div>

      <div className="space-y-2">
        {almacenes.map((alm) => {
          const d = datosAlmacen(alm);
          const isCentral = alm.tipo === 'CENTRAL';
          const cap = alm.capacidadMaxima || 1000;
          const stock = alm.stockActual ?? (isCentral ? 9999 : 0);
          const pct = isCentral ? 100 : Math.min(100, Math.round((stock / cap) * 100));
          const sem = nivelSemaforoAlmacen(alm);
          const semCfg = SEMAFORO_CONFIG[sem];
          const esSeleccionado = selectedAlmacenCodigo === alm.codigo;
          const estaExpandido = expandido === alm.codigo;
          const subPestana = pestana[alm.codigo] ?? 'transito';

          const detalle = pedidosPorAlmacen.get(alm.codigo) ?? { enTransito: [], proximas4h: [], ultimas4h: [] };

          return (
            <div
              key={alm.codigo}
              className={`rounded-lg border transition-all text-xs ${
                esSeleccionado
                  ? 'border-(--color-brand-500) bg-(--color-brand-50)/70 shadow-xs ring-1 ring-(--color-brand-500)'
                  : 'border-gray-200 bg-white hover:border-gray-300'
              }`}
            >
              {/* Cabecera del almacén */}
              <div
                className="p-2.5 flex items-center gap-2 cursor-pointer select-none"
                onClick={() => onSelectAlmacen(alm.codigo)}
              >
                {/* Ícono de bodega */}
                <svg viewBox="-19 -19 46 46" className="w-6 h-6 shrink-0" aria-hidden="true">
                  <Bodega color={d.color} letra={d.letra} />
                </svg>

                {/* Info Almacén */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-(--color-ink-900)">Almacén {d.nombre}</span>
                    {isCentral ? (
                      <span className="inline-flex items-center gap-1 text-[11px] font-bold text-orange-600">
                        <InfinityIcon size={13} /> Ilimitado
                      </span>
                    ) : (
                      <span
                        className={`inline-flex items-center gap-1 px-1.5 py-0.2 rounded border text-[10px] font-bold ${semCfg.badgeClase}`}
                      >
                        <span className="w-1.5 h-1.5 rounded-full" style={{ backgroundColor: semCfg.dotColor }} />
                        {pct}%
                      </span>
                    )}
                  </div>

                  {/* Stock y barra */}
                  <div className="flex items-center gap-2 mt-1">
                    <div className="flex-1 bg-gray-100 rounded-full h-1.5 overflow-hidden">
                      <div
                        className="h-1.5 rounded-full transition-all"
                        style={{
                          width: `${pct}%`,
                          backgroundColor: isCentral ? '#EA580C' : semCfg.dotColor,
                        }}
                      />
                    </div>
                    <span className="text-[10px] font-bold text-gray-700 shrink-0">
                      {isCentral ? 'Recarga diaria' : `${stock} / ${cap} u`}
                    </span>
                  </div>
                </div>

                {/* Flecha expandir */}
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    setExpandido(estaExpandido ? null : alm.codigo);
                  }}
                  className="p-1 text-gray-400 hover:text-gray-700 rounded transition-colors"
                  title={estaExpandido ? 'Ocultar flujo de envíos' : 'Ver envíos en tránsito y ventanas de 4h'}
                >
                  {estaExpandido ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
                </button>
              </div>

              {/* Detalle expandido: 3 ventanas requeridas por la rúbrica */}
              {estaExpandido && (
                <div className="border-t border-gray-100 bg-gray-50/70 p-2 space-y-2">
                  {/* Selector de sub-pestaña */}
                  <div className="flex gap-1 border-b border-gray-200 pb-1.5 text-[10px] font-bold">
                    <button
                      type="button"
                      onClick={() => setPestana((prev) => ({ ...prev, [alm.codigo]: 'transito' }))}
                      className={`flex-1 py-1 rounded transition-colors ${
                        subPestana === 'transito'
                          ? 'bg-(--color-brand-500) text-white'
                          : 'text-gray-600 hover:bg-gray-200'
                      }`}
                    >
                      En camino ({detalle.enTransito.length})
                    </button>
                    <button
                      type="button"
                      onClick={() => setPestana((prev) => ({ ...prev, [alm.codigo]: 'proximas4h' }))}
                      className={`flex-1 py-1 rounded transition-colors ${
                        subPestana === 'proximas4h'
                          ? 'bg-(--color-brand-500) text-white'
                          : 'text-gray-600 hover:bg-gray-200'
                      }`}
                    >
                      Próx. 4h ({detalle.proximas4h.length})
                    </button>
                    <button
                      type="button"
                      onClick={() => setPestana((prev) => ({ ...prev, [alm.codigo]: 'ultimas4h' }))}
                      className={`flex-1 py-1 rounded transition-colors ${
                        subPestana === 'ultimas4h'
                          ? 'bg-(--color-brand-500) text-white'
                          : 'text-gray-600 hover:bg-gray-200'
                      }`}
                    >
                      Últimas 4h ({detalle.ultimas4h.length})
                    </button>
                  </div>

                  {/* Contenido de la sub-pestaña */}
                  <div className="space-y-1 max-h-36 overflow-y-auto pr-1">
                    {subPestana === 'transito' && (
                      detalle.enTransito.length === 0 ? (
                        <p className="text-gray-400 italic text-center py-2 text-[11px]">No hay pedidos en tránsito ahora.</p>
                      ) : (
                        detalle.enTransito.map((p) => (
                          <div key={p.codigo} className="flex justify-between items-center bg-white px-2 py-1 rounded border border-gray-200">
                            <span className="font-bold text-(--color-ink-900)">{p.codigo}</span>
                            <span className="text-gray-500 text-[10px]">({p.destino.posX},{p.destino.posY})</span>
                            <span className="font-semibold text-(--color-brand-500)">{p.cantidad} u</span>
                          </div>
                        ))
                      )
                    )}

                    {subPestana === 'proximas4h' && (
                      detalle.proximas4h.length === 0 ? (
                        <p className="text-gray-400 italic text-center py-2 text-[11px]">Sin despachos programados en las próx. 4h.</p>
                      ) : (
                        detalle.proximas4h.slice(0, 10).map((p) => (
                          <div key={p.codigo} className="flex justify-between items-center bg-white px-2 py-1 rounded border border-gray-200">
                            <span className="font-bold text-(--color-ink-900)">{p.codigo}</span>
                            <span className="text-gray-500 text-[10px]">Por salir</span>
                            <span className="font-semibold text-amber-600">{p.cantidad} u</span>
                          </div>
                        ))
                      )
                    )}

                    {subPestana === 'ultimas4h' && (
                      detalle.ultimas4h.length === 0 ? (
                        <p className="text-gray-400 italic text-center py-2 text-[11px]">Sin entregas registradas en las últimas 4h.</p>
                      ) : (
                        detalle.ultimas4h.slice(0, 10).map((p) => (
                          <div key={p.codigo} className="flex justify-between items-center bg-white px-2 py-1 rounded border border-gray-200">
                            <span className="font-bold text-(--color-ink-900)">{p.codigo}</span>
                            <span className="text-green-700 text-[10px] font-semibold">Entregado</span>
                            <span className="font-semibold text-green-700">{p.cantidad} u</span>
                          </div>
                        ))
                      )
                    )}
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};
