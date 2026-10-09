import React, { useEffect, useState } from 'react';
import { Sidebar } from './components/layout/Sidebar';
import { Header } from './components/layout/Header';
import { MapaOperaciones } from './components/mapa/MapaOperaciones';
import { RightPanel } from './components/panel/RightPanel';
import { RegistroPage } from './components/registro/RegistroPage';
import { ConfiguracionPage } from './components/configuracion/ConfiguracionPage';
import { OperacionDiariaPage } from './components/operacion/OperacionDiariaPage';
import { PantallaPendiente } from './components/simulacion/PantallaPendiente';
import { ReportePage } from './components/reportes/ReportePage';
import { Play, Pause, Square, ChevronLeft, ChevronRight } from 'lucide-react';

import { useAlmacenes } from './hooks/useAlmacenes';
import { useSimulacion } from './hooks/useSimulacion';
import { useFlota } from './hooks/useFlota';
import { useMapaOperaciones } from './hooks/useMapaOperaciones';
import { useMetricas } from './hooks/useMetricas';
import { usePersistente } from './hooks/usePersistente';
import { NOMBRE_ESCENARIO, esEscenario } from './types/escenarios';
import type { Escenario } from './types/escenarios';
import type { BloqueoVial, RelojSimuladoData, Ubicacion } from './types';
import { bloqueosApi } from './api/bloqueosApi';
import { rutasApi } from './api/rutasApi';
import type { RutaApi } from './api/rutasApi';
import type { RutaMapa } from './components/mapa/MapaOperaciones';

type Seccion = 'registro' | 'configuracion' | 'simulacion' | 'reportes';

// Estilo de los botones de control de la simulación (▶ ⏸ ■)
const botonControl =
  'w-11 h-9 flex items-center justify-center rounded-md border border-(--color-brand-500) bg-white text-(--color-ink-900) hover:bg-(--color-brand-50) transition-colors disabled:opacity-40 disabled:cursor-not-allowed';

export const App: React.FC = () => {
  const [activeSection, setActiveSection] = useState<Seccion>('configuracion');
  const [speed, setSpeed] = useState('1');

  const { almacenes } = useAlmacenes(5000);
  const {
    reloj,
    iniciar,
    detener,
    finalizarSimulacion,
    estaPausada,
    cambiandoPausa,
    reanudar,
    alternarPausa,
  } = useSimulacion(3000);
  const { unidades, selectedUnidadCodigo, setSelectedUnidadCodigo } = useFlota(4000);
  const { pedidos } = useMapaOperaciones(4000);
  const { metricas } = useMetricas(5000);

  // Bloqueos para dibujarlos en el mapa (el mapa solo muestra los que están activos ahora)
  const [bloqueos, setBloqueos] = useState<BloqueoVial[]>([]);
  useEffect(() => {
    let vivo = true;
    const cargar = () =>
      bloqueosApi
        .listar()
        .then((lista) => { if (vivo) setBloqueos(lista); })
        .catch(() => { /* si falla, se reintenta en la próxima consulta */ });
    cargar();
    const t = setInterval(cargar, 5000);
    return () => { vivo = false; clearInterval(t); };
  }, []);

  // Rutas planificadas para dibujarlas en el mapa
  const [rutasApiData, setRutasApiData] = useState<RutaApi[]>([]);
  useEffect(() => {
    let vivo = true;
    const cargar = () => rutasApi.listar().then((lista) => { if (vivo) setRutasApiData(lista); });
    cargar();
    const t = setInterval(cargar, 5000);
    return () => { vivo = false; clearInterval(t); };
  }, []);

  // Rutas vigentes = todas las que no están COMPLETADAS (incluye las REPLANIFICADAS).
  // Si un vehículo tiene varias, se usa la más reciente (id mayor) para no dibujar líneas repetidas.
  const rutaVigentePorUnidad = new Map<string, RutaApi>();
  for (const r of rutasApiData) {
    if (r.estado === 'COMPLETADA') continue;
    const actual = rutaVigentePorUnidad.get(r.unidadCodigo);
    if (!actual || r.id > actual.id) rutaVigentePorUnidad.set(r.unidadCodigo, r);
  }

  // Solo el tramo que falta recorrer de cada ruta.
  const mismaUbicacion = (a: Ubicacion, b: Ubicacion) => a.posX === b.posX && a.posY === b.posY;
  const rutasMapa: RutaMapa[] = Array.from(rutaVigentePorUnidad.values())
    .map((r) => {
      const unidad = unidades.find((u) => u.codigo === r.unidadCodigo);
      if (unidad?.estadoOperativo === 'AVERIADO') return null;

      const paradas = [...r.paradas].filter((p) => p.destino).sort((a, b) => a.orden - b.orden);
      const paradasPendientes = paradas.filter((p) => !p.entregada);
      const codigosPedidos = paradasPendientes.map((p) => p.codigoPedido);
      if (paradasPendientes.length === 0) return null;

      // Si el backend envía el camino real se usa ese, porque refleja las calles que de verdad recorre
      const camino = r.camino ?? [];
      if (camino.length >= 2) {
        // Se ubica cada parada en el camino para separar lo ya recorrido de lo que falta
        let desde = 0;       // índice de la última parada ya entregada (o el almacén)
        let hasta = -1;      // índice de la próxima parada pendiente
        let buscarDesde = 0;
        for (const p of paradas) {
          const i = camino.findIndex((c, k) => k >= buscarDesde && mismaUbicacion(c, p.destino));
          if (i === -1) continue;
          buscarDesde = i;
          if (p.entregada) desde = i;
          else if (hasta === -1) hasta = i;
        }
        // Se arranca en la esquina más cercana al vehículo para no dibujar el tramo que ya pasó
        let inicio = desde;
        if (unidad?.ubicacionActual && hasta > desde) {
          let mejor = Infinity;
          for (let k = desde; k <= hasta; k++) {
            const d = Math.abs(camino[k].posX - unidad.ubicacionActual.posX) + Math.abs(camino[k].posY - unidad.ubicacionActual.posY);
            if (d < mejor) { mejor = d; inicio = k; }
          }
        }
        const puntos = camino.slice(inicio);
        return puntos.length >= 2 ? { unidadCodigo: r.unidadCodigo, puntos, codigosPedidos, esCamino: true } : null;
      }

      // Si no llegó camino, se une el vehículo con sus paradas pendientes para no perder la ruta
      const pendientes = paradasPendientes.map((p) => p.destino);
      const puntos = unidad?.ubicacionActual ? [unidad.ubicacionActual, ...pendientes] : pendientes;
      return puntos.length >= 2 ? { unidadCodigo: r.unidadCodigo, puntos, codigosPedidos } : null;
    })
    .filter((r): r is RutaMapa => r !== null);

  // Configuración elegida (se recuerda al recargar la página)
  const [escenarioSel, setEscenarioSel] = usePersistente<Escenario>('paqrap.escenarioSel', 'DIA_A_DIA', esEscenario);
  const [fechaInicio, setFechaInicio] = usePersistente<string>('paqrap.fechaInicio', '2026-08-11');
  const [horaInicio, setHoraInicio] = usePersistente<string>('paqrap.horaInicio', '06:00');

  // Panel derecho de la simulación: se puede ocultar para que el mapa ocupe más espacio
  const [panelAbierto, setPanelAbierto] = usePersistente<boolean>(
    'paqrap.panelDerechoAbierto',
    true,
    (v) => typeof v === 'boolean',
  );

  // El backend tiene UN solo motor y sus datos (vehículos, pedidos, stock) pertenecen
  // al ÚLTIMO escenario que lo inició, aunque ya haya terminado o se haya detenido.
  // Recordamos cuál fue (el backend todavía no lo informa en /api/simulacion/reloj).
  const [escenarioMotor, setEscenarioMotor] = usePersistente<Escenario | null>(
    'paqrap.escenarioMotor',
    null,
    (v) => v === null || esEscenario(v),
  );

  const motorCorriendo = reloj.estadoEjecucion === 'EN_EJECUCION';
  // "Activo" = corriendo o en pausa: la simulación sigue viva y se puede reanudar o finalizar.
  const motorActivo = motorCorriendo || estaPausada;

  // Si el backend informa el escenario (campo "escenario" de /api/simulacion/reloj) se usa ese;
  // si no, se usa el que recordó este navegador.
  const escenarioBackend = (reloj as RelojSimuladoData & { escenario?: string | null }).escenario;
  const duenoDatos: Escenario | null = esEscenario(escenarioBackend) ? escenarioBackend : escenarioMotor;

  // Una pantalla solo muestra datos si son de SU escenario. Si el motor está activo y no se sabe
  // quién lo inició (p. ej. se limpió el navegador), tampoco se muestran.
  const sinDatos = (duenoDatos !== null && duenoDatos !== escenarioSel) || (motorActivo && duenoDatos === null);
  const ocupadoPor = duenoDatos ? NOMBRE_ESCENARIO[duenoDatos] : null;

  const arrancar = async (escenario: Escenario) => {
    setEscenarioMotor(escenario);
    try {
      await iniciar(escenario);
    } catch {
      // El motor puede tardar en responder; el reloj se sigue consultando solo.
    }
  };

  // ⏸: corriendo → pausa; en pausa → reanuda (no finaliza la simulación).
  const pausarOReanudar = async () => {
    await alternarPausa();
  };

  const play = async () => {
    if (estaPausada) {
      await reanudar();
    } else {
      await arrancar(escenarioSel);
    }
  };

  // Detener el motor (comportamiento anterior, se mantiene para la pantalla de Operación día a día).
  const detenerMotor = async () => {
    try {
      await detener();
    } catch {
      // Si falla, el estado real lo trae el reloj en la próxima consulta.
    }
  };

  // Finalizar: termina la simulación y lleva a la página de Reportes.
  const terminar = async () => {
    await finalizarSimulacion();
    setActiveSection('reportes');
  };

  const irASimulacion = (escenario: Escenario) => {
    setEscenarioSel(escenario);
    setActiveSection('simulacion');
  };

  const formatFecha = (isoString: string) => {
    if (!isoString) return '';
    const date = new Date(isoString);
    return date.toLocaleString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  };

  // Tiempo transcurrido = instante actual − instante en que inició la simulación
  // (no la hora del reloj: si empezó a las 06:00, a las 07:30 lleva 1 h 30 min).
  const getTranscurrido = () => {
    if (!reloj.instanteActual || !reloj.instanteInicio) return '0 h';
    const ms = new Date(reloj.instanteActual).getTime() - new Date(reloj.instanteInicio).getTime();
    if (!Number.isFinite(ms) || ms < 0) return '0 h';
    const totalMin = Math.floor(ms / 60000);
    const horas = Math.floor(totalMin / 60);
    const minutos = totalMin % 60;
    return minutos > 0 ? `${horas} h ${minutos} min` : `${horas} h`;
  };

  const diaActual = reloj.diaSimulado || 1;

  const titulos: Record<Seccion, { title: string; subtitle?: string }> = {
    registro: { title: 'Registro', subtitle: 'Gestiona la información de pedidos, avería y bloqueos para la simulación' },
    configuracion: { title: 'Configuración de la simulación', subtitle: 'Parámetros previos a ejecutar la simulación' },
    simulacion: { title: NOMBRE_ESCENARIO[escenarioSel] },
    reportes: { title: 'Reporte de la simulación', subtitle: 'Resumen de desempeño de la última corrida ejecutada' },
  };

  return (
    <div className="flex min-h-screen bg-(--color-surface-50) text-(--color-ink-900)">
      <Sidebar activeSection={activeSection} onSelectSection={(s) => setActiveSection(s as Seccion)} />

      <div className="flex-1 flex flex-col min-w-0">
        <Header
          onFinalizarSimulacion={terminar}
          totalAlertas={0}
          isSimulating={motorActivo && !sinDatos}
          userName="Jones Ferdinand"
          title={titulos[activeSection].title}
          subtitle={titulos[activeSection].subtitle}
          showFinalizar={activeSection === 'simulacion'}
        />

        {activeSection === 'registro' && <RegistroPage />}

        {activeSection === 'configuracion' && (
          <ConfiguracionPage
            onSimulacionIniciada={(e) => irASimulacion(e as Escenario)}
            // Si el branch de Figma eliminó las props viejas de ConfiguracionPage, solo pasamos esta.
            // Si TypeScript se queja luego por props faltantes, las agregaremos, pero usaremos la firma de Figma.
          />
        )}

        {activeSection === 'reportes' && <ReportePage />}

        {activeSection === 'simulacion' && (
          escenarioSel === 'DIA_A_DIA' ? (
            <OperacionDiariaPage
              reloj={reloj}
              almacenes={almacenes}
              unidades={unidades}
              pedidos={pedidos}
              selectedUnidadId={selectedUnidadCodigo}
              onSelectUnidad={setSelectedUnidadCodigo}
              sinDatos={sinDatos}
              ocupadoPor={ocupadoPor}
              onIniciar={() => arrancar('DIA_A_DIA')}
              onDetener={detenerMotor}
              onFinalizar={terminar}
            />
          ) : (
            <main className="flex-1 p-6 md:p-8 flex flex-col w-full h-[calc(100vh-72px)] overflow-hidden">
              {/* Barra superior (sin tarjeta: va directo sobre el fondo, como el mockup) */}
              <div className="flex flex-wrap justify-between items-center gap-3 mb-4 px-1 text-sm">
                <div className="flex flex-wrap items-center gap-x-10 gap-y-2">
                  <div className="text-(--color-ink-400) font-semibold">
                    Fecha: <span className="text-(--color-ink-900) font-bold ml-1">{sinDatos ? '--' : formatFecha(reloj.instanteActual)}</span>
                  </div>
                  <div className="flex items-center text-(--color-ink-400) font-semibold">
                    <ClockIcon className="w-4 h-4 mr-1.5" /> Tiempo transcurrido:
                    <span className="text-(--color-ink-900) font-bold ml-1">{sinDatos ? '--' : getTranscurrido()}</span>
                  </div>
                  <div className="text-(--color-ink-400) font-semibold">
                    Día: <span className="text-(--color-ink-900) font-bold ml-1">{diaActual}</span>
                  </div>
                  {estaPausada && !sinDatos && (
                    <span className="rounded-full bg-amber-50 border border-amber-200 px-2.5 py-0.5 text-xs font-bold text-amber-700">
                      En pausa
                    </span>
                  )}
                </div>

                <div className="flex items-center gap-2">
                  {sinDatos && (
                    <span className="text-xs font-semibold text-amber-600 mr-2">
                      {motorActivo
                        ? `El motor está ocupado con ${ocupadoPor ?? 'otro escenario'}, por eso aquí no se muestran datos.`
                        : `Los datos actuales son de ${ocupadoPor ?? 'otra simulación'}, por eso aquí no se muestran. Presiona ▶ para iniciar.`}
                    </span>
                  )}
                  {/* ▶ Iniciar (sin simulación) o continuar (en pausa) */}
                  <button
                    onClick={play}
                    disabled={motorCorriendo || cambiandoPausa}
                    title={estaPausada ? 'Continuar simulación' : 'Iniciar simulación'}
                    aria-label={estaPausada ? 'Continuar simulación' : 'Iniciar simulación'}
                    className={botonControl}
                  >
                    <Play size={16} fill="currentColor" />
                  </button>

                  {/* ⏸ Pausa; si ya está en pausa queda marcado y otro clic continúa */}
                  <button
                    onClick={pausarOReanudar}
                    disabled={!motorActivo || cambiandoPausa}
                    title={estaPausada ? 'En pausa: clic para continuar' : 'Pausar simulación'}
                    aria-label={estaPausada ? 'Continuar simulación' : 'Pausar simulación'}
                    aria-pressed={estaPausada}
                    className={`${botonControl} ${estaPausada ? 'bg-(--color-brand-100)' : ''}`}
                  >
                    <Pause size={16} fill="currentColor" />
                  </button>

                  {/* ■ Finaliza y lleva a Reportes */}
                  <button
                    onClick={terminar}
                    disabled={!motorActivo}
                    title="Finalizar simulación"
                    aria-label="Finalizar simulación"
                    className={botonControl}
                  >
                    <Square size={16} fill="currentColor" />
                  </button>

                  <select
                    value={speed}
                    onChange={(e) => setSpeed(e.target.value)}
                    aria-label="Velocidad de la simulación"
                    className="h-9 bg-white border border-(--color-brand-500) text-(--color-ink-900) rounded-md px-2 text-sm font-bold outline-none"
                  >
                    <option value="0.5">0.5×</option>
                    <option value="1">1.0×</option>
                    <option value="2">2.0×</option>
                  </select>
                </div>
              </div>

              {/* El mapa mantiene su tamaño. Al abrir el panel, mapa y panel se corren juntos
                  a la izquierda (el borde izquierdo del mapa sale de la vista); al cerrarlo, vuelven. */}
              <div className="flex-1 min-h-0">
              <div className="relative h-full overflow-hidden rounded-xl">
                <div
                  className={`absolute inset-0 flex transition-transform duration-300 ease-in-out ${
                    panelAbierto ? '-translate-x-[21rem]' : 'translate-x-0'
                  }`}
                >
                  {/* Mapa: siempre del ancho completo del área */}
                  <div className="w-full h-full shrink-0">
                    <MapaOperaciones
                      almacenes={almacenes}
                      unidades={sinDatos ? [] : unidades}
                      pedidos={sinDatos ? [] : pedidos}
                      bloqueos={sinDatos ? [] : bloqueos}
                      rutas={sinDatos ? [] : rutasMapa}
                      selectedUnidadCodigo={selectedUnidadCodigo}
                      onSelectUnidad={setSelectedUnidadCodigo}
                      escenario={NOMBRE_ESCENARIO[escenarioSel]}
                    />
                  </div>

                  {/* Panel derecho: queda a la derecha del mapa, fuera de vista cuando está cerrado */}
                  <div className="w-80 h-full shrink-0 ml-4" aria-hidden={!panelAbierto}>
                    <RightPanel
                      reloj={reloj}
                      almacenes={sinDatos ? [] : almacenes}
                      unidades={sinDatos ? [] : unidades}
                      pedidos={sinDatos ? [] : pedidos}
                      metricas={metricas}
                      selectedUnidadCodigo={selectedUnidadCodigo}
                    />
                  </div>
                </div>

                {/* Pestaña discreta: entre el mapa y el panel (abierto) o en el borde derecho (cerrado) */}
                <button
                  onClick={() => setPanelAbierto(!panelAbierto)}
                  title={panelAbierto ? 'Ocultar panel' : 'Mostrar panel'}
                  aria-label={panelAbierto ? 'Ocultar panel lateral' : 'Mostrar panel lateral'}
                  aria-expanded={panelAbierto}
                  className={`absolute top-1/2 -translate-y-1/2 z-30 w-4 h-14 flex items-center justify-center rounded-l-md bg-white border border-r-0 border-(--color-line-200) text-(--color-ink-400) hover:text-(--color-brand-500) shadow-sm transition-[right,color] duration-300 ease-in-out ${
                    panelAbierto ? 'right-80' : 'right-0'
                  }`}
                >
                  {panelAbierto ? <ChevronRight size={14} /> : <ChevronLeft size={14} />}
                </button>
              </div>
              </div>
            </main>
          )
        )}
      </div>
    </div>
  );
};

const ClockIcon = ({ className }: { className?: string }) => (
  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className={className}><circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" /></svg>
);

export default App;