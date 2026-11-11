import React, { useState } from 'react';
import { ChevronDown, ChevronUp, AlertCircle, AlertTriangle, CheckCircle2, ClipboardList, Car as CarIcon, Siren, Wrench, Save, Loader2 } from 'lucide-react';
import { RelojSimuladoData, Almacen, UnidadTransporte, Pedido, MetricasSimulacion, ESTADO_PEDIDO_LABEL } from '../../types';
import type { TipoAveria } from '../../types/registro';
import { averiasApi } from '../../api/averiasApi';
import { formatFechaHora, parseUbicacion } from '../registro/utils';
import { Eye, EyeOff, Layers, Warehouse } from 'lucide-react';
import { Bodega, VehiculoRealista } from '../mapa/IconosMapa';
import { FILTROS_VACIOS, alternarCapa, capaAlmacen, type CapaMapa, type FiltrosPanel, type SemaforoUT } from '../../hooks/filtros';
import { ListaUnidadesPanel } from './ListaUnidadesPanel';
import { ListaAlmacenesPanel } from './ListaAlmacenesPanel';

const TIPOS_AVERIA: { valor: TipoAveria; etiqueta: string }[] = [
  { valor: 'TIPO_1', etiqueta: 'Tipo 1 — 2 h en el lugar' },
  { valor: 'TIPO_2', etiqueta: 'Tipo 2 — hasta cambio de turno' },
  { valor: 'TIPO_3', etiqueta: 'Tipo 3 — ≥ 2 días' },
];

const selectBase =
  'w-full rounded-lg border border-(--color-ink-300) bg-white px-3 py-2 text-sm text-(--color-ink-900) outline-none focus:border-(--color-brand-500) focus:ring-2 focus:ring-(--color-brand-100) disabled:bg-gray-100';

interface RightPanelProps {
  // Se mantiene en la interfaz por compatibilidad con App.tsx; la fecha simulada ya se muestra
  // en la barra superior de la pantalla, por eso el panel ya no la repite.
  reloj: RelojSimuladoData;
  almacenes: Almacen[];
  unidades: UnidadTransporte[];
  pedidos: Pedido[];
  metricas: MetricasSimulacion;
  selectedUnidadCodigo: string | null;
  selectedAlmacenCodigo?: string | null;
  onSelectUnidad?: (codigo: string) => void;
  onSelectAlmacen?: (codigo: string) => void;
  rutas?: Array<{ unidadCodigo: string; codigosPedidos?: string[] }>;
  filtros?: FiltrosPanel;
  onCambiarFiltros?: (filtros: FiltrosPanel) => void;
  /**
   * 'operacion': panel de la Operación día a día (registro de avería, resumen, vehículos, stock, incidencias y costo).
   * 'simulacion' (por defecto): además muestra el detalle de vehículos y pedidos.
   */
  modo?: 'operacion' | 'simulacion';
}

/** Tipos de vehículo que se pueden mostrar u ocultar en el mapa */
// Mismos colores e íconos que usa el mapa, para que panel y mapa se lean igual
const CAPAS_VEHICULO: { capa: CapaMapa; etiqueta: string; tipo: string; color: string }[] = [
  { capa: 'Auto', etiqueta: 'Autos', tipo: 'Auto', color: '#2563EB' },
  { capa: 'Moto', etiqueta: 'Motos', tipo: 'Moto', color: '#9333EA' },
  { capa: 'Bicicleta', etiqueta: 'Bicicletas', tipo: 'Bicicleta', color: '#EC4899' },
];

/** Datos del almacén igual que en el mapa: nombre corto, letra y color */
const datosAlmacen = (a: Almacen) => {
  if (a.tipo === 'CENTRAL') return { nombre: 'Central', letra: 'C', color: '#EA580C' };
  const esNorte = /n[-\s]?o/i.test(a.nombre) || a.nombre.toLowerCase().includes('norte');
  return esNorte ? { nombre: 'Norte', letra: 'N', color: '#0891B2' } : { nombre: 'Este', letra: 'E', color: '#4F46E5' };
};

/** Ícono del vehículo tal como se dibuja en el mapa (visto desde arriba) */
const MiniVehiculo: React.FC<{ tipo: string; color: string }> = ({ tipo, color }) => (
  <svg viewBox="-20 -20 40 40" className="w-5 h-5 shrink-0" aria-hidden="true">
    <VehiculoRealista tipo={tipo} color={color} />
  </svg>
);

/** Ícono del almacén tal como se dibuja en el mapa (bodega con su letra) */
const MiniBodega: React.FC<{ letra: string; color: string }> = ({ letra, color }) => (
  <svg viewBox="-19 -19 46 46" className="w-5 h-5 shrink-0" aria-hidden="true">
    <Bodega color={color} letra={letra} />
  </svg>
);

/**
 * Fila de una capa. El ojito es el control:
 * - visible: ojo gris suave (no distrae)
 * - oculto: ojo tachado en color, y la fila se atenúa, así se nota de un vistazo qué se apagó
 */
const FilaCapa: React.FC<{ etiqueta: string; detalle?: string; icono: React.ReactNode; visible: boolean; onAlternar: () => void }> = ({
  etiqueta, detalle, icono, visible, onAlternar,
}) => (
  <div className="group flex items-center gap-2 rounded-md px-1.5 py-1 hover:bg-gray-50 transition-colors">
    <span className={`transition-opacity ${visible ? '' : 'opacity-35 grayscale'}`}>{icono}</span>
    <span className={`flex-1 min-w-0 truncate text-[13px] font-semibold ${visible ? 'text-(--color-ink-900)' : 'text-(--color-ink-500)'}`}>
      {etiqueta}
    </span>
    {detalle && <span className="text-[11px] text-(--color-ink-500) shrink-0">{detalle}</span>}
    <button
      type="button"
      onClick={onAlternar}
      aria-pressed={visible}
      aria-label={visible ? `Ocultar ${etiqueta} del mapa` : `Mostrar ${etiqueta} en el mapa`}
      title={visible ? `Ocultar ${etiqueta} del mapa` : `Mostrar ${etiqueta} en el mapa`}
      className={`shrink-0 rounded p-0.5 transition-colors ${
        visible
          ? 'text-(--color-ink-400) group-hover:text-(--color-ink-500) hover:bg-gray-100'
          : 'text-(--color-brand-500) bg-(--color-brand-50) hover:bg-(--color-brand-100)'
      }`}
    >
      {visible ? <Eye size={14} /> : <EyeOff size={14} />}
    </button>
  </div>
);

/** Título de una columna */
const TituloGrupo: React.FC<{ titulo: string }> = ({ titulo }) => (
  <div className="px-1.5 pb-1 border-b border-gray-100 text-xs font-bold uppercase tracking-wide text-(--color-ink-500)">{titulo}</div>
);

/**
 * Tarjeta del panel: todas comparten el mismo encabezado (ícono azul + título en negrita + flecha).
 * Abierta: borde azul suave y encabezado celeste, para saber cuál estás viendo.
 * "extra" se muestra a la derecha del título (por ejemplo, el enlace "Ver todo").
 */
const Tarjeta: React.FC<{
  titulo: string;
  Icono: React.ElementType;
  abierta: boolean;
  onToggle: () => void;
  extra?: React.ReactNode;
  children: React.ReactNode;
}> = ({ titulo, Icono, abierta, onToggle, extra, children }) => (
  <div
    className={`rounded-xl border bg-white shadow-sm overflow-hidden transition-colors ${
      abierta ? 'border-(--color-brand-500)/40' : 'border-(--color-line-200)'
    }`}
  >
    <div className={`flex items-center gap-2 pr-3 transition-colors ${abierta ? 'bg-(--color-brand-50)' : 'hover:bg-gray-50'}`}>
      <button
        type="button"
        onClick={onToggle}
        aria-expanded={abierta}
        className="flex-1 min-w-0 flex items-center gap-2 pl-4 py-3 text-left"
      >
        <Icono size={16} className="text-(--color-brand-500) shrink-0" />
        <span className="flex-1 min-w-0 truncate text-sm font-bold text-(--color-ink-900)">{titulo}</span>
      </button>
      {extra}
      <button type="button" onClick={onToggle} aria-label={abierta ? `Cerrar ${titulo}` : `Abrir ${titulo}`} className="shrink-0 p-0.5">
        {abierta ? <ChevronUp size={18} className="text-(--color-brand-500)" /> : <ChevronDown size={18} className="text-(--color-ink-500)" />}
      </button>
    </div>
    {abierta && <div className="px-4 pt-3 pb-4">{children}</div>}
  </div>
);

/** Ícono "S/" (la operación es en soles; el ícono de dólar no corresponde) */
const IconoSoles: React.FC<{ size?: number; className?: string }> = ({ size = 16, className = '' }) => (
  <span
    className={`inline-flex items-center justify-center font-extrabold leading-none shrink-0 ${className}`}
    style={{ width: size, height: size, fontSize: size * 0.68 }}
    aria-hidden="true"
  >
    S/
  </span>
);

// Costo por km de cada tipo de vehículo (enunciado): auto S/ 8, moto S/ 6, bicicleta S/ 3
const COSTO_KM: Record<string, number> = { Auto: 8, Moto: 6, Bicicleta: 3 };

// Criticidad de los pedidos (mismos colores que los pines del mapa)
const CRITICIDAD = [
  { valor: 'VERDE', texto: 'A tiempo', color: '#22B957' },
  { valor: 'AMBAR', texto: 'En riesgo', color: '#F5B900' },
  { valor: 'ROJO', texto: 'Crítico', color: '#E53935' },
];

/** Lee el primer campo con valor entre varios nombres posibles (el backend puede llamarlos distinto) */
const campo = (obj: object, nombres: string[]): unknown => {
  const o = obj as Record<string, unknown>;
  for (const n of nombres) if (o[n] !== undefined && o[n] !== null && o[n] !== '') return o[n];
  return undefined;
};

const formatSoles = (valor: number) => `S/ ${valor.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

export const RightPanel: React.FC<RightPanelProps> = ({
  reloj,
  almacenes,
  unidades,
  pedidos,
  metricas,
  selectedUnidadCodigo,
  selectedAlmacenCodigo = null,
  onSelectUnidad = () => {},
  onSelectAlmacen = () => {},
  rutas = [],
  filtros: filtrosProp,
  onCambiarFiltros,
}) => {
  // El detalle de cada vehículo, almacén y pedido se ve en el mapa: al hacer clic en su ícono se abre un popup.
  // Tarjetas abiertas (pueden ser varias). Al inicio solo "Mostrar en el mapa".
  const [abiertas, setAbiertas] = useState<Set<string>>(() => new Set(['mapa']));
  const [filtrosLocal, setFiltrosLocal] = useState<FiltrosPanel>(FILTROS_VACIOS);
  const filtros = filtrosProp ?? filtrosLocal;
  const setFiltros = (f: FiltrosPanel) => (onCambiarFiltros ? onCambiarFiltros(f) : setFiltrosLocal(f));
  const visible = (capa: CapaMapa) => !filtros.ocultos.includes(capa);
  const nOcultos = filtros.ocultos.length;

  // --- Registro de avería durante la simulación ---
  const [unidadAveria, setUnidadAveria] = useState('');
  const [tipoAveria, setTipoAveria] = useState<TipoAveria>('TIPO_1');
  const [registrandoAveria, setRegistrandoAveria] = useState(false);
  const [avisoAveria, setAvisoAveria] = useState<{ ok: boolean; texto: string } | null>(null);

  // Ejemplo para el placeholder: el código de una unidad real, si hay datos
  const ejemploUnidad = unidades.find(u => u.activo)?.codigo ?? 'A3';

  const registrarAveria = async () => {
    const codigo = unidadAveria.trim().toUpperCase();
    if (!codigo) {
      setAvisoAveria({ ok: false, texto: 'Escribe el código de la unidad averiada.' });
      return;
    }
    const unidad = unidades.find(u => u.codigo.toUpperCase() === codigo);
    if (!unidad || !unidad.activo) {
      setAvisoAveria({ ok: false, texto: `No existe una unidad activa con el código ${codigo}.` });
      return;
    }
    if (unidad.estadoOperativo === 'AVERIADA') {
      setAvisoAveria({ ok: false, texto: `La unidad ${unidad.codigo} ya está averiada.` });
      return;
    }
    // La avería ocurre donde está la unidad en este momento de la simulación
    const ubic = unidad.ubicacionActual
      ? parseUbicacion(`(${unidad.ubicacionActual.posX},${unidad.ubicacionActual.posY})`)
      : null;
    if (!ubic) {
      setAvisoAveria({ ok: false, texto: 'No se conoce la ubicación actual de la unidad.' });
      return;
    }

    setRegistrandoAveria(true);
    setAvisoAveria(null);
    try {
      const registrada = await averiasApi.registrar({ idUnidad: unidad.codigo, ubicacion: ubic, tipo: tipoAveria });
      setAvisoAveria({
        ok: true,
        texto: registrada.horaReincorporacion
          ? `Avería registrada en ${unidad.codigo}. Vuelve el ${formatFechaHora(registrada.horaReincorporacion)}.`
          : `Avería registrada en ${unidad.codigo}.`,
      });
      setUnidadAveria('');
      setTipoAveria('TIPO_1');
    } catch {
      setAvisoAveria({ ok: false, texto: 'No se pudo registrar la avería. Inténtalo de nuevo.' });
    } finally {
      setRegistrandoAveria(false);
    }
  };

  const toggleSection = (section: string) => {
    setAbiertas((prev) => {
      const sig = new Set(prev);
      if (sig.has(section)) sig.delete(section);
      else sig.add(section);
      return sig;
    });
  };

  const autos = unidades.filter(u => u.tipoNombre === 'Auto');
  const motos = unidades.filter(u => u.tipoNombre === 'Moto');
  const bicis = unidades.filter(u => u.tipoNombre === 'Bicicleta');

  const getCounts = (list: UnidadTransporte[]) => ({
    disp: list.filter(u => u.estadoOperativo === 'DISPONIBLE').length,
    ruta: list.filter(u => u.estadoOperativo === 'EN_RUTA').length,
    ave: list.filter(u => u.estadoOperativo === 'AVERIADA' || u.estadoOperativo === 'EN_MANTENIMIENTO').length,
    tot: list.length,
  });

  const aCount = getCounts(autos);
  const mCount = getCounts(motos);
  const bCount = getCounts(bicis);

  const totalPedidos = pedidos.length;
  const entregados = pedidos.filter(p => p.estado === 'ENTREGADO').length;
  const enRuta = pedidos.filter(p => p.estado === 'EN_RUTA').length;
  const planificados = pedidos.filter(p => p.estado === 'PLANIFICADO').length;
  const porAtender = pedidos.filter(p => p.estado === 'REGISTRADO').length;

  const pctCumplimiento = totalPedidos > 0 ? (entregados / totalPedidos) * 100 : 0;
  const reasignados = pedidos.filter(p => (p.estado as string) === 'REASIGNADO').length;

  // Pedidos aún no entregados, por criticidad (lo más importante del caso: entregar dentro del plazo)
  const activos = pedidos.filter(p => p.estado !== 'ENTREGADO');
  const porCriticidad = (c: string) => activos.filter(p => p.criticidad === c).length;
  const enRiesgo = activos
    .filter(p => p.criticidad === 'ROJO' || p.criticidad === 'AMBAR')
    .sort((a, b) => (a.criticidad === b.criticidad ? 0 : a.criticidad === 'ROJO' ? -1 : 1));

  // Costo por tipo, km recorridos (costo ÷ S/ por km) y costo por entrega
  const costos = [
    { tipo: 'Auto', etiqueta: 'Autos', costo: metricas.costoAuto, color: '#2563EB' },
    { tipo: 'Moto', etiqueta: 'Motos', costo: metricas.costoMoto, color: '#9333EA' },
    { tipo: 'Bicicleta', etiqueta: 'Bicicletas', costo: metricas.costoBici, color: '#EC4899' },
  ].map(c => ({ ...c, km: c.costo / COSTO_KM[c.tipo] }));
  const costoTotal = metricas.costoAcumuladoTotal;
  const kmTotal = costos.reduce((s, c) => s + c.km, 0);
  const costoPorEntrega = entregados > 0 ? costoTotal / entregados : 0;

  return (
    <div className="w-full h-full flex-shrink-0 space-y-3 overflow-y-auto pr-2 scrollbar-thin">
      {metricas.instanteColapso && (
        <div className="bg-red-50 border border-red-300 rounded-xl p-3 text-center mb-2">
          <div className="flex items-center justify-center gap-2 text-red-700 font-bold text-sm">
            <AlertTriangle size={16} /> Colapso logístico
          </div>
          <p className="text-[13px] text-red-600 mt-1">
            Un pedido incumplió su plazo. Simulación detenida
            {typeof metricas.volumenPedidosColapso === 'number' && (
              <> con {metricas.volumenPedidosColapso} pedido(s) activos en ese instante.</>
            )}
          </p>
        </div>
      )}

      {/* ===== Mostrar en el mapa: encender o apagar cada tipo de vehículo y cada almacén ===== */}
      <Tarjeta
        titulo="Mostrar en el mapa"
        Icono={Layers}
        abierta={abiertas.has('mapa')}
        onToggle={() => toggleSection('mapa')}
        extra={
          nOcultos > 0 ? (
            <button
              type="button"
              onClick={() => setFiltros(FILTROS_VACIOS)}
              className="shrink-0 text-xs font-semibold text-(--color-brand-500) hover:underline"
              title="Volver a mostrar todo"
            >
              Ver todo ({nOcultos})
            </button>
          ) : undefined
        }
      >
        {/* Dos columnas: vehículos a la izquierda y almacenes a la derecha */}
        <div className="grid grid-cols-[1.15fr_1fr] gap-3">
          <div className="space-y-0.5">
            <TituloGrupo titulo="Vehículos" />
            {CAPAS_VEHICULO.map(({ capa, etiqueta, tipo, color }) => (
              <FilaCapa
                key={capa}
                etiqueta={etiqueta}
                detalle={`${unidades.filter((u) => u.tipoNombre === capa).length}`}
                icono={<MiniVehiculo tipo={tipo} color={color} />}
                visible={visible(capa)}
                onAlternar={() => setFiltros(alternarCapa(filtros, capa))}
              />
            ))}
          </div>

          <div className="space-y-0.5">
            <TituloGrupo titulo="Almacenes" />
            {almacenes.map((alm) => {
              const d = datosAlmacen(alm);
              return (
                <FilaCapa
                  key={alm.codigo}
                  etiqueta={d.nombre}
                  icono={<MiniBodega letra={d.letra} color={d.color} />}
                  visible={visible(capaAlmacen(alm))}
                  onAlternar={() => setFiltros(alternarCapa(filtros, capaAlmacen(alm)))}
                />
              );
            })}
          </div>
        </div>

        {/* Filtro global por semáforo de ocupación de UT (Sí3) */}
        <div className="pt-2.5 mt-2.5 border-t border-gray-100">
          <span className="block text-[11px] font-bold text-(--color-ink-500) mb-1.5 uppercase tracking-wide">
            Filtrar unidades por semáforo de carga
          </span>
          <div className="flex items-center gap-1">
            {(['TODOS', 'VACIO', 'VERDE', 'AMBAR', 'ROJO'] as const).map((sem) => {
              const activo = (filtros.semaforoUT ?? 'TODOS') === sem;
              return (
                <button
                  key={sem}
                  type="button"
                  onClick={() => setFiltros({ ...filtros, semaforoUT: sem })}
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
        </div>
      </Tarjeta>

      {/* Registrar avería: en la operación diaria y en las simulaciones, porque las averías ocurren en plena marcha */}
      {/* Registrar avería (siempre visible; las averías se registran manualmente durante la simulación) */}
      <Tarjeta titulo="Registrar avería" Icono={Wrench} abierta={abiertas.has('averia')} onToggle={() => toggleSection('averia')}>
      <div className="space-y-3">

        <label className="block">
          <span className="block text-[13px] font-semibold text-(--color-ink-500) mb-1">Unidad</span>
          <input
            type="text"
            value={unidadAveria}
            onChange={(e) => { setUnidadAveria(e.target.value); setAvisoAveria(null); }}
            onKeyDown={(e) => { if (e.key === 'Enter') registrarAveria(); }}
            placeholder={`Ej. ${ejemploUnidad}`}
            disabled={registrandoAveria}
            className={selectBase}
          />
        </label>

        <label className="block">
          <span className="block text-[13px] font-semibold text-(--color-ink-500) mb-1">Tipo de avería</span>
          <select
            value={tipoAveria}
            onChange={(e) => setTipoAveria(e.target.value as TipoAveria)}
            disabled={registrandoAveria}
            className={selectBase}
          >
            {TIPOS_AVERIA.map(t => (
              <option key={t.valor} value={t.valor}>{t.etiqueta}</option>
            ))}
          </select>
        </label>

        <p className="text-xs text-(--color-ink-500)">
          Se registra en la ubicación actual de la unidad y con la hora de la simulación.
        </p>

        <button
          type="button"
          onClick={registrarAveria}
          disabled={!unidadAveria.trim() || registrandoAveria}
          className="w-full flex items-center justify-center gap-2 rounded-lg bg-(--color-brand-500) hover:bg-(--color-brand-600) disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold px-4 py-2 text-sm transition-colors"
        >
          {registrandoAveria ? <Loader2 size={16} className="animate-spin" /> : <Save size={16} />}
          {registrandoAveria ? 'Registrando...' : 'Registrar avería'}
        </button>

        {avisoAveria && (
          <p className={`text-[13px] rounded-md px-2.5 py-2 ${avisoAveria.ok ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'}`}>
            {avisoAveria.texto}
          </p>
        )}
      </div>
      </Tarjeta>


      {/* ===== Resumen de pedidos: avance, etapas y criticidad de lo que falta entregar ===== */}
      <Tarjeta titulo="Resumen de pedidos" Icono={ClipboardList} abierta={abiertas.has('resumen')} onToggle={() => toggleSection('resumen')}>
        <div className="space-y-3">
          <div className="flex items-center gap-2">
            <CheckCircle2 size={18} className="text-green-600" />
            <span className="font-bold text-(--color-ink-900)">Entregados</span>
            <span className="ml-auto font-bold text-(--color-brand-500) text-lg">{pctCumplimiento.toFixed(1)} %</span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-2">
            <div className="bg-(--color-brand-500) h-2 rounded-full" style={{ width: `${pctCumplimiento}%` }}></div>
          </div>
          <p className="text-xs text-(--color-ink-500) -mt-1">{entregados} de {totalPedidos} pedidos</p>

          <div className="grid grid-cols-2 gap-x-4 gap-y-1.5 text-[13px] text-(--color-ink-500)">
            <div className="flex justify-between"><span>{ESTADO_PEDIDO_LABEL.REGISTRADO}</span> <span className="font-semibold text-(--color-ink-900)">{porAtender}</span></div>
            <div className="flex justify-between"><span>{ESTADO_PEDIDO_LABEL.PLANIFICADO}</span> <span className="font-semibold text-(--color-ink-900)">{planificados}</span></div>
            <div className="flex justify-between"><span>{ESTADO_PEDIDO_LABEL.EN_RUTA}</span> <span className="font-semibold text-(--color-ink-900)">{enRuta}</span></div>
            <div className="flex justify-between"><span>Reasignados</span> <span className="font-semibold text-(--color-ink-900)">{reasignados}</span></div>
          </div>

          {/* Criticidad de los pedidos que faltan entregar (semáforo, igual que los pines del mapa) */}
          <div className="pt-2 border-t border-gray-100">
            <span className="block text-xs font-semibold text-(--color-ink-500) mb-1.5">Pendientes por criticidad ({activos.length})</span>
            <div className="grid grid-cols-3 gap-1.5">
              {CRITICIDAD.map(c => (
                <div key={c.valor} className="rounded-lg border border-gray-100 px-2 py-1.5 text-center">
                  <div className="text-base font-bold" style={{ color: c.color }}>{porCriticidad(c.valor)}</div>
                  <div className="text-[11px] text-(--color-ink-500)">{c.texto}</div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </Tarjeta>

      {/* ===== Pedidos en riesgo: los que hay que vigilar para no incumplir el plazo ===== */}
      <Tarjeta
        titulo={`Pedidos en riesgo (${enRiesgo.length})`}
        Icono={Siren}
        abierta={abiertas.has('riesgo')}
        onToggle={() => toggleSection('riesgo')}
      >
        {enRiesgo.length === 0 ? (
          <p className="text-[13px] text-(--color-ink-500) text-center py-2">Ningún pedido en riesgo de incumplir su plazo.</p>
        ) : (
          <div className="space-y-1 max-h-56 overflow-y-auto -mx-1 px-1">
            {enRiesgo.map(p => {
              const c = CRITICIDAD.find(x => x.valor === p.criticidad);
              const limite = campo(p, ['fechaHoraLimite', 'fechaLimite', 'plazoLimite', 'horaLimite', 'fechaEntregaLimite']);
              return (
                <div key={p.codigo} className="flex items-center gap-2 rounded-md px-1.5 py-1 text-[13px] hover:bg-gray-50">
                  <span className="w-2 h-2 rounded-full shrink-0" style={{ backgroundColor: c?.color }} />
                  <span className="font-semibold text-(--color-ink-900) w-20 truncate">{p.codigo}</span>
                  <span className="text-(--color-ink-500) flex-1 truncate">{ESTADO_PEDIDO_LABEL[p.estado] ?? p.estado}</span>
                  {limite !== undefined && (
                    <span className="text-[11px] text-(--color-ink-500) shrink-0">
                      {new Date(String(limite)).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' })}
                    </span>
                  )}
                  <span className="text-[11px] font-semibold shrink-0" style={{ color: c?.color }}>{c?.texto}</span>
                </div>
              );
            })}
          </div>
        )}
      </Tarjeta>

      {/* ===== Vehículos: resumen y lista detallada de UT con semáforo y pedidos ===== */}
      <Tarjeta titulo={`Unidades de Transporte (${unidades.length})`} Icono={CarIcon} abierta={abiertas.has('vehiculos')} onToggle={() => toggleSection('vehiculos')}>
        <div className="space-y-3">
          <table className="w-full text-[13px] text-(--color-ink-500) text-center">
            <thead>
              <tr className="border-b border-gray-100">
                <th className="text-left py-1 font-semibold"></th>
                <th className="py-1 font-semibold">Disponible</th>
                <th className="py-1 font-semibold">En ruta</th>
                <th className="py-1 font-semibold">Averiada</th>
                <th className="py-1 font-bold text-(--color-ink-900)">Total</th>
              </tr>
            </thead>
            <tbody>
              {([
                ['Auto', 'Autos', aCount],
                ['Moto', 'Motos', mCount],
                ['Bicicleta', 'Bicicletas', bCount],
              ] as const).map(([tipo, etiqueta, c], i) => (
                <tr key={tipo} className={i < 2 ? 'border-b border-gray-50' : ''}>
                  <td className="text-left py-1.5" title={etiqueta}>
                    <MiniVehiculo tipo={tipo} color={CAPAS_VEHICULO.find(v => v.tipo === tipo)?.color ?? '#64748B'} />
                  </td>
                  <td>{c.disp}</td><td>{c.ruta}</td><td>{c.ave}</td><td className="font-bold text-(--color-ink-900)">{c.tot}</td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="pt-2 border-t border-gray-100">
            <ListaUnidadesPanel
              unidades={unidades}
              pedidos={pedidos}
              rutas={rutas}
              selectedUnidadCodigo={selectedUnidadCodigo}
              onSelectUnidad={onSelectUnidad}
              filtroSemaforo={filtros.semaforoUT}
              onCambiarFiltroSemaforo={(sem) => setFiltros({ ...filtros, semaforoUT: sem })}
            />
          </div>
        </div>
      </Tarjeta>

      {/* ===== Almacenes: Lista detallada con stock, semáforo y flujos de 4h ===== */}
      <Tarjeta titulo="Almacenes e Inventario" Icono={Warehouse} abierta={abiertas.has('almacenes')} onToggle={() => toggleSection('almacenes')}>
        <ListaAlmacenesPanel
          almacenes={almacenes}
          pedidos={pedidos}
          reloj={reloj}
          selectedAlmacenCodigo={selectedAlmacenCodigo}
          onSelectAlmacen={onSelectAlmacen}
        />
      </Tarjeta>

      {/* ===== Incidencias ===== */}
      <Tarjeta titulo="Incidencias" Icono={AlertTriangle} abierta={abiertas.has('incidencias')} onToggle={() => toggleSection('incidencias')}>
        <div className="space-y-3 text-sm text-(--color-ink-700)">
          <div className="flex justify-between items-center bg-gray-50 p-2 rounded-lg">
            <span className="flex items-center text-red-600 font-medium"><AlertCircle size={16} className="mr-2" /> Bloqueos ocurridos</span>
            <span className="font-bold">{metricas.bloqueosOcurridos}</span>
          </div>
          <div className="flex justify-between items-center bg-gray-50 p-2 rounded-lg">
            <span className="flex items-center text-(--color-ink-700) font-medium"><Wrench size={16} className="mr-2" /> Averías ocurridas</span>
            <span className="font-bold">{metricas.averiasOcurridas}</span>
          </div>
        </div>
      </Tarjeta>

      {/* ===== Costo acumulado (en soles): total, reparto por tipo, km y costo por entrega ===== */}
      <Tarjeta titulo="Costo acumulado" Icono={IconoSoles} abierta={abiertas.has('costo')} onToggle={() => toggleSection('costo')}>
        <div className="space-y-3 text-[13px]">
          <div className="text-center">
            <div className="text-2xl font-extrabold text-(--color-ink-900)">{formatSoles(costoTotal)}</div>
            <div className="text-xs text-(--color-ink-500)">{kmTotal.toLocaleString('es-PE', { maximumFractionDigits: 1 })} km recorridos</div>
          </div>

          <div className="space-y-2">
            {costos.map(c => (
              <div key={c.tipo} className="flex items-center gap-2">
                <MiniVehiculo tipo={c.tipo} color={c.color} />
                <span className="w-16 text-(--color-ink-700) font-medium">{c.etiqueta}</span>
                <div className="flex-1 bg-gray-100 rounded-full h-1.5 overflow-hidden">
                  <div className="h-1.5 rounded-full" style={{ width: costoTotal > 0 ? `${(c.costo / costoTotal) * 100}%` : 0, backgroundColor: c.color }} />
                </div>
                <span className="w-20 text-right font-semibold text-(--color-ink-900)">{formatSoles(c.costo)}</span>
              </div>
            ))}
          </div>

          <div className="grid grid-cols-2 gap-2 pt-2 border-t border-gray-100">
            <div className="rounded-lg bg-gray-50 px-2 py-1.5 text-center">
              <div className="font-bold text-(--color-ink-900)">{formatSoles(costoPorEntrega)}</div>
              <div className="text-[11px] text-(--color-ink-500)">por pedido entregado</div>
            </div>
            <div className="rounded-lg bg-gray-50 px-2 py-1.5 text-center">
              <div className="font-bold text-(--color-ink-900)">{kmTotal > 0 ? formatSoles(costoTotal / kmTotal) : formatSoles(0)}</div>
              <div className="text-[11px] text-(--color-ink-500)">promedio por km</div>
            </div>
          </div>
          <p className="text-[11px] text-(--color-ink-500)">Tarifa por km: auto S/ 8 · moto S/ 6 · bicicleta S/ 3.</p>
        </div>
      </Tarjeta>

    </div>
  );
};