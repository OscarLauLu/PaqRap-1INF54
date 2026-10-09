import React, { useState } from 'react';
import { ChevronDown, ChevronUp, Clock, MapPin, Truck, AlertCircle, AlertTriangle, DollarSign, Bike as BikeIcon, Car as CarIcon, Wrench, Save, Loader2 } from 'lucide-react';
import { RelojSimuladoData, Almacen, UnidadTransporte, Pedido, MetricasSimulacion, ESTADO_PEDIDO_LABEL } from '../../types';
import type { TipoAveria } from '../../types/registro';
import { averiasApi } from '../../api/averiasApi';
import { formatFechaHora, parseUbicacion } from '../registro/utils';

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
}

const formatSoles = (valor: number) => `S/ ${valor.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

export const RightPanel: React.FC<RightPanelProps> = ({ almacenes, unidades, pedidos, metricas, selectedUnidadCodigo }) => {
  const [expandedSection, setExpandedSection] = useState<string | null>('resumen');

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
    if (unidad.estadoOperativo === 'AVERIADO') {
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
    setExpandedSection(prev => (prev === section ? null : section));
  };

  const selectedUnidad = selectedUnidadCodigo ? unidades.find(u => u.codigo === selectedUnidadCodigo) : null;
  const autos = unidades.filter(u => u.tipoNombre === 'Auto');
  const motos = unidades.filter(u => u.tipoNombre === 'Moto');
  const bicis = unidades.filter(u => u.tipoNombre === 'Bicicleta');

  const getCounts = (list: UnidadTransporte[]) => ({
    disp: list.filter(u => u.estadoOperativo === 'DISPONIBLE').length,
    ruta: list.filter(u => u.estadoOperativo === 'EN_RUTA').length,
    ave: list.filter(u => u.estadoOperativo === 'AVERIADO' || u.estadoOperativo === 'EN_MANTENIMIENTO').length,
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

  const Accordion = ({ title, id, children }: { title: string; id: string; children: React.ReactNode }) => (
    <div className="bg-white rounded-xl shadow-sm border border-(--color-line-200) overflow-hidden mb-2">
      <button
        className="w-full px-4 py-3 flex justify-between items-center text-(--color-ink-700) font-semibold hover:bg-gray-50/50"
        onClick={() => toggleSection(id)}
      >
        <span className="text-sm">{title}</span>
        {expandedSection === id ? <ChevronUp size={18} className="text-(--color-ink-400)" /> : <ChevronDown size={18} className="text-(--color-ink-400)" />}
      </button>
      {expandedSection === id && <div className="px-4 pb-4 bg-white">{children}</div>}
    </div>
  );

  return (
    <div className="w-80 h-full flex-shrink-0 space-y-3 overflow-y-auto pr-2 scrollbar-thin">
      {metricas.instanteColapso && (
        <div className="bg-red-50 border border-red-300 rounded-xl p-3 text-center mb-2">
          <div className="flex items-center justify-center gap-2 text-red-700 font-bold text-sm">
            <AlertTriangle size={16} /> Colapso logístico
          </div>
          <p className="text-xs text-red-600 mt-1">
            {metricas.volumenPedidosColapso ?? 1} pedido{(metricas.volumenPedidosColapso ?? 1) !== 1 ? 's' : ''} ya no pueden garantizarse dentro de su plazo.<br/>
            <strong>Simulación detenida</strong>
          </p>
        </div>
      )}

      {/* Registrar avería (siempre visible; las averías se registran manualmente durante la simulación) */}
      <div className="bg-white rounded-xl shadow-sm border border-(--color-line-200) p-4 mb-2 space-y-3">
        <div className="flex items-center gap-2 text-sm font-bold text-(--color-ink-900)">
          <Wrench size={16} className="text-(--color-brand-500)" /> Registrar avería
        </div>

        <label className="block">
          <span className="block text-xs font-semibold text-(--color-ink-500) mb-1">Unidad</span>
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
          <span className="block text-xs font-semibold text-(--color-ink-500) mb-1">Tipo de avería</span>
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

        <p className="text-[11px] text-(--color-ink-400)">
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
          <p className={`text-xs rounded-md px-2.5 py-2 ${avisoAveria.ok ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'}`}>
            {avisoAveria.texto}
          </p>
        )}
      </div>

      <Accordion title="Resumen de pedidos" id="resumen">
        <div className="space-y-3">
          <div className="flex items-center space-x-2">
            <Clock size={20} className="text-(--color-ink-700)" />
            <span className="font-bold text-(--color-ink-900) text-lg">Cumplimiento</span>
            <span className="ml-auto font-bold text-(--color-brand-500) text-lg">{pctCumplimiento.toFixed(1)} %</span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-2">
            <div className="bg-(--color-brand-500) h-2 rounded-full" style={{ width: `${pctCumplimiento}%` }}></div>
          </div>
          <div className="pt-2 space-y-1.5 text-xs text-(--color-ink-500)">
            <div className="flex justify-between font-bold text-sm text-(--color-ink-900)"><span className="font-semibold">Total</span> <span className="text-(--color-brand-500)">{totalPedidos}</span></div>
            <div className="flex justify-between"><span>{ESTADO_PEDIDO_LABEL.REGISTRADO}</span> <span>{porAtender}</span></div>
            <div className="flex justify-between"><span>{ESTADO_PEDIDO_LABEL.PLANIFICADO}</span> <span>{planificados}</span></div>
            <div className="flex justify-between"><span>{ESTADO_PEDIDO_LABEL.EN_RUTA}</span> <span>{enRuta}</span></div>
            <div className="flex justify-between"><span>{ESTADO_PEDIDO_LABEL.ENTREGADO}</span> <span>{entregados}</span></div>
          </div>
        </div>
      </Accordion>

      <Accordion title="Vehículos" id="vehiculos">
        <table className="w-full text-xs text-(--color-ink-500) text-center">
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
            <tr className="border-b border-gray-50">
              <td className="text-left py-1.5"><CarIcon size={14} className="text-(--color-ink-700)" /></td>
              <td>{aCount.disp}</td><td>{aCount.ruta}</td><td>{aCount.ave}</td><td className="font-bold text-(--color-ink-900)">{aCount.tot}</td>
            </tr>
            <tr className="border-b border-gray-50">
              <td className="text-left py-1.5"><Truck size={14} className="text-(--color-ink-700)" /></td>
              <td>{mCount.disp}</td><td>{mCount.ruta}</td><td>{mCount.ave}</td><td className="font-bold text-(--color-ink-900)">{mCount.tot}</td>
            </tr>
            <tr>
              <td className="text-left py-1.5"><BikeIcon size={14} className="text-(--color-ink-700)" /></td>
              <td>{bCount.disp}</td><td>{bCount.ruta}</td><td>{bCount.ave}</td><td className="font-bold text-(--color-ink-900)">{bCount.tot}</td>
            </tr>
          </tbody>
        </table>
      </Accordion>

      <Accordion title="Stock de almacenes" id="almacenes">
        <div className="space-y-2 text-xs">
          {almacenes.map(alm => (
            <div key={alm.codigo} className="border border-(--color-brand-100) rounded-lg p-2 flex justify-between items-center text-(--color-brand-700) font-semibold bg-white">
              <span className="flex items-center gap-1"><MapPin size={12} />{alm.nombre}</span>
              <span>
                {alm.stockActual === null ? (
                  <span className="flex items-center"><span className="text-(--color-brand-500) mr-1 text-[16px]">∞</span> Ilimitado</span>
                ) : (
                  `${alm.stockActual} / ${alm.capacidadMaxima ?? 1000}`
                )}
              </span>
            </div>
          ))}
        </div>
      </Accordion>

      <Accordion title="Incidencias" id="incidencias">
        <div className="space-y-3 text-sm text-(--color-ink-700)">
          <div className="flex justify-between items-center bg-gray-50 p-2 rounded-lg">
            <span className="flex items-center text-red-600 font-medium"><AlertCircle size={16} className="mr-2" /> Bloqueos ocurridos</span>
            <span className="font-bold">{metricas.bloqueosOcurridos}</span>
          </div>
          <div className="flex justify-between items-center bg-gray-50 p-2 rounded-lg">
            <span className="flex items-center text-(--color-ink-700) font-medium"><AlertTriangle size={16} className="mr-2" /> Averías ocurridas</span>
            <span className="font-bold">{metricas.averiasOcurridas}</span>
          </div>
        </div>
      </Accordion>

      <Accordion title="Costo acumulado" id="costo">
        <div className="space-y-1.5 text-xs text-(--color-ink-500)">
          <div className="flex items-center justify-center gap-2 text-2xl font-extrabold text-(--color-ink-900) py-1">
            <DollarSign size={20} className="text-(--color-brand-500)" />
            {formatSoles(metricas.costoAcumuladoTotal)}
          </div>
          <div className="flex justify-between"><span>Autos</span><span>{formatSoles(metricas.costoAuto)}</span></div>
          <div className="flex justify-between"><span>Motos</span><span>{formatSoles(metricas.costoMoto)}</span></div>
          <div className="flex justify-between"><span>Bicicletas</span><span>{formatSoles(metricas.costoBici)}</span></div>
        </div>
      </Accordion>

      <Accordion title="Detalles del vehículo" id="detalle_vehiculo">
        {selectedUnidad ? (
          <div className="text-xs text-(--color-ink-500) space-y-2">
            <div className="flex items-center bg-(--color-brand-50) rounded-lg p-2 mb-3">
              <div className="bg-(--color-brand-100) p-2 rounded-md mr-3">
                <Truck size={20} className="text-(--color-brand-700)" />
              </div>
              <div>
                <div className="font-bold text-(--color-ink-900) text-sm">{selectedUnidad.tipoNombre} {selectedUnidad.codigo}</div>
                <div className="text-(--color-brand-500) font-semibold">{selectedUnidad.estadoOperativo.replace('_', ' ')}</div>
              </div>
            </div>
            <div className="flex justify-between"><span className="font-medium text-(--color-ink-400)">Capacidad utilizada</span> <span>{selectedUnidad.porcentajeCarga.toFixed(1)}%</span></div>
            <div className="flex justify-between"><span className="font-medium text-(--color-ink-400)">Carga</span> <span>{selectedUnidad.cargaActual}/{selectedUnidad.capacidadMaxima}</span></div>
            <div className="flex justify-between"><span className="font-medium text-(--color-ink-400)">Velocidad</span> <span>{selectedUnidad.velocidadPromedioKmH} km/h</span></div>
            <div className="flex justify-between"><span className="font-medium text-(--color-ink-400)">Costo</span> <span>{formatSoles(selectedUnidad.costoPorKm)}/km</span></div>
            {selectedUnidad.nombreConductor && (
              <div className="flex justify-between"><span className="font-medium text-(--color-ink-400)">Conductor</span> <span>{selectedUnidad.nombreConductor}</span></div>
            )}
          </div>
        ) : (
          <div className="text-xs text-(--color-ink-400) text-center py-2">Selecciona un vehículo en el mapa</div>
        )}
      </Accordion>

      <Accordion title="Detalles de pedido" id="detalle_pedido">
        <div className="text-xs text-(--color-ink-400) text-center py-2">Selecciona un pedido en el mapa</div>
      </Accordion>
    </div>
  );
};
