import React, { useEffect, useState } from 'react';
import { Sidebar } from './components/layout/Sidebar';
import { Header } from './components/layout/Header';
import { MapaOperaciones } from './components/mapa/MapaOperaciones';
import { RightPanel } from './components/panel/RightPanel';
import { RegistroPage } from './components/registro/RegistroPage';
import { ConfiguracionPage } from './components/configuracion/ConfiguracionPage';
import { ReportePage } from './components/reportes/ReportePage';
import { AlertaColapso } from './components/simulacion/AlertaColapso';
import { Play, Pause, Square, ChevronLeft, ChevronRight } from 'lucide-react';
import { setBackend } from './api/client';
import { simulacionApi } from './api/simulacionApi';
import { FILTROS_VACIOS, type FiltrosPanel } from './hooks/filtros';

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
import { parsearFechaLocal } from './utils/fechaLocal';

type Seccion = 'registro' | 'configuracion' | 'simulacion' | 'reportes';

// Estilo de los botones de control de la simulación (▶ ⏸ ■)
const botonControl =
  'w-11 h-9 flex items-center justify-center rounded-md border border-brand-500 bg-white text-ink-900 hover:bg-brand-50 transition-colors disabled:opacity-40 disabled:cursor-not-allowed';

export const App: React.FC = () => {
  const [activeSection, setActiveSection] = useState<Seccion>('configuracion');
  const [speed, setSpeed] = useState('1');

  useEffect(() => {
    try {
      localStorage.removeItem('paqrap.activeSection');
    } catch {
      // ignorar
    }
  }, []);

  const [filtros, setFiltros] = useState<FiltrosPanel>(FILTROS_VACIOS);

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
  } = useSimulacion(1000);
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

  // Rutas vigentes = todas las que no están COMPLETADAS (incluye las REPLANIFICADAS, que son la misma
  // ruta con otra etiqueta). Si el planificador le encadenó varias a un vehículo, se dibuja la que está
  // en curso: la que sale primero. Es la que el motor está moviendo y la que define su carga actual.
  const salidaDe = (r: RutaApi) => parsearFechaLocal(r.fechaHoraGeneracion) ?? Number.MAX_SAFE_INTEGER;
  const rutaVigentePorUnidad = new Map<string, RutaApi>();
  for (const r of rutasApiData) {
    if (r.estado === 'COMPLETADA') continue;   // una ruta con todo entregado sigue vigente mientras regresa
    const actual = rutaVigentePorUnidad.get(r.unidadCodigo);
    const antes = actual && (salidaDe(r) < salidaDe(actual) || (salidaDe(r) === salidaDe(actual) && r.id < actual.id));
    if (!actual || antes) rutaVigentePorUnidad.set(r.unidadCodigo, r);
  }

  // Solo el tramo que falta recorrer de cada ruta.
  const mismaUbicacion = (a: Ubicacion, b: Ubicacion) => a.posX === b.posX && a.posY === b.posY;
  const rutasMapa: RutaMapa[] = Array.from(rutaVigentePorUnidad.values())
    .map((r): RutaMapa | null => {
      const unidad = unidades.find((u) => u.codigo === r.unidadCodigo);
      if (unidad?.estadoOperativo === 'AVERIADA') return null;

      const paradas = [...(r.paradas || [])].filter((p) => p.destino).sort((a, b) => a.orden - b.orden);
      const paradasPendientes = paradas.filter((p) => !p.entregada);
      const codigosPedidos: string[] = paradasPendientes
        .map((p) => p.codigoPedido)
        .filter((c): c is string => c !== undefined && c !== null);
      // Sin paradas pendientes el vehículo está regresando al almacén: su ruta se sigue mostrando
      const regresando = paradasPendientes.length === 0;

      // Si el backend envía el camino real se usa ese, porque refleja las calles que de verdad recorre
      const camino = r.camino ?? [];
      if (camino.length >= 2) {
        // Se ubica cada parada en el camino para separar lo ya recorrido de lo que falta
        let desde = 0;       // índice de la última parada ya entregada (o el almacén)
        let hasta = -1;      // índice de la próxima parada pendiente
        let buscarDesde = 0;
        // Horario de la ruta sobre el camino: el mapa calcula con él dónde va el vehículo en cada cuadro
        const salida = parsearFechaLocal(r.fechaHoraGeneracion);
        const horarioParadas: NonNullable<RutaMapa['horario']>['paradas'] = [];
        let horarioCompleto = salida !== null;
        // Ruta de rescate: antes de las entregas va al punto de la avería y trasvasa (30 min)
        if (r.puntoTrasvase) {
          const punto = r.puntoTrasvase;
          const i = camino.findIndex((c, k) => k >= 1 && mismaUbicacion(c, punto));
          const llegada = parsearFechaLocal(r.llegadaTrasvase);
          const fin = parsearFechaLocal(r.finTrasvase);
          if (i === -1 || llegada === null || fin === null) horarioCompleto = false;
          else {
            horarioParadas.push({ idx: i, llegada, salida: fin });
            buscarDesde = i;
          }
        }
        for (const p of paradas) {
          const i = camino.findIndex((c, k) => k >= buscarDesde && mismaUbicacion(c, p.destino));
          if (i === -1) { horarioCompleto = false; continue; }
          buscarDesde = i;
          if (p.entregada) desde = i;
          else if (hasta === -1) hasta = i;
          const llegada = parsearFechaLocal(p.horaEstimadaLlegada);
          if (llegada === null) { horarioCompleto = false; continue; }
          horarioParadas.push({ idx: i, llegada, salida: llegada + (p.tiempoServicioMin ?? 60) * 60_000 });
        }
        const retorno = parsearFechaLocal(r.horaLlegadaAlmacen) ?? undefined;
        const horario = horarioCompleto && salida !== null ? { salida, paradas: horarioParadas, retorno } : undefined;
        // Se arranca en la esquina más cercana al vehículo para no dibujar el tramo que ya pasó
        let inicio = desde;
        const tope = hasta === -1 ? camino.length - 1 : hasta;   // regresando: hasta el almacén
        if (unidad?.ubicacionActual && tope > desde) {
          let mejor = Infinity;
          for (let k = desde; k <= tope; k++) {
            const d = Math.abs(camino[k].posX - unidad.ubicacionActual.posX) + Math.abs(camino[k].posY - unidad.ubicacionActual.posY);
            if (d < mejor) { mejor = d; inicio = k; }
          }
        }
        const puntos = camino.slice(inicio);
        // caminoCompleto: el mapa anima al vehículo recorriendo este camino, para que no cruce manzanas en diagonal
        return puntos.length >= 2
          ? { unidadCodigo: r.unidadCodigo, puntos, codigosPedidos, esCamino: true, caminoCompleto: camino, horario, rescataA: r.rescataA ?? undefined }
          : null;
      }

      // Sin camino no se puede dibujar el regreso; el vehículo se ve en la posición que informa el backend
      if (regresando) return null;

      // Si no llegó camino, se une el vehículo con sus paradas pendientes para no perder la ruta
      const pendientes = paradasPendientes.map((p) => p.destino);
      const puntos = unidad?.ubicacionActual ? [unidad.ubicacionActual, ...pendientes] : pendientes;
      return puntos.length >= 2 ? { unidadCodigo: r.unidadCodigo, puntos, codigosPedidos } : null;
    })
    .filter((r): r is RutaMapa => r !== null);

  // Configuración elegida (se recuerda al recargar la página)
  const [escenarioSel, setEscenarioSel] = usePersistente<Escenario>('paqrap.escenarioSel', 'DIA_A_DIA', esEscenario);

  // Cada pantalla le habla a su propio backend: Registro y Operación día a día al de operación;
  // Configuración, Simulación 5D, Colapso y Reportes al de simulación
  const backend =
    activeSection === 'registro' || (activeSection === 'simulacion' && escenarioSel === 'DIA_A_DIA')
      ? 'operacion'
      : 'simulacion';
  setBackend(backend);

  // La Operación día a día usa la misma pantalla y los mismos controles que las simulaciones.
  // Diferencias: no tiene botón rojo Finalizar, su ■ solo la detiene (no lleva a Reportes,
  // que muestran las simulaciones) y su panel tiene menos tarjetas
  const esOperacion = escenarioSel === 'DIA_A_DIA';

  // Panel derecho de la simulación: se puede ocultar para que el mapa ocupe más espacio
  // Siempre arranca cerrado al entrar a la pantalla (el mapa se ve completo)
  const [panelAbierto, setPanelAbierto] = useState(false);

  // El backend de simulación tiene UN solo motor y sus datos pertenecen al ÚLTIMO escenario
  // que lo inició (5D o colapso). Recordamos cuál fue.
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

  // Una pantalla de simulación solo muestra datos si son de SU escenario (5D o colapso).
  // La operación tiene su propio backend, así que nunca está "ocupada" por otro escenario.
  const sinDatos = backend !== 'operacion' && ((duenoDatos !== null && duenoDatos !== escenarioSel) || (motorActivo && duenoDatos === null));
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

  // Finalizar: termina la simulación y lleva a la página de Reportes.
  const terminar = async () => {
    // Se espera como máximo 3 segundos a que el backend confirme que terminó;
    // pase lo que pase, después se va a Reportes para que el botón nunca parezca no responder
    await Promise.race([finalizarSimulacion(), new Promise((resolver) => setTimeout(resolver, 3000))]);
    setActiveSection('reportes');
  };

  // ■ en la operación diaria: la detiene y se queda en la pantalla (▶ la vuelve a iniciar)
  const detenerOperacion = async () => {
    try {
      await detener();
    } catch {
      // Si falla, el estado real lo trae el reloj en la próxima consulta.
    }
  };

  const irASimulacion = (escenario: Escenario) => {
    setEscenarioSel(escenario);
    setActiveSection('simulacion');
  };

  const formatFecha = (isoString: string) => {
    if (!isoString) return '';
    const date = new Date(isoString);
    return date.toLocaleString('es-PE', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  };

  // Tiempo transcurrido = instante actual − instante en que inició la simulación
  // Muestra segundos, minutos y horas para dar visibilidad inmediata del avance.
  const getTranscurrido = () => {
    if (!reloj.instanteActual || !reloj.instanteInicio) return '0 s';
    const ms = new Date(reloj.instanteActual).getTime() - new Date(reloj.instanteInicio).getTime();
    if (!Number.isFinite(ms) || ms < 0) return '0 s';
    const totalSeg = Math.floor(ms / 1000);
    const totalMin = Math.floor(totalSeg / 60);
    const horas = Math.floor(totalMin / 60);
    const minutos = totalMin % 60;
    const segundos = totalSeg % 60;
    if (horas > 0) {
      return minutos > 0 ? `${horas} h ${minutos} min` : `${horas} h`;
    }
    if (minutos > 0) {
      return segundos > 0 ? `${minutos} min ${segundos} s` : `${minutos} min`;
    }
    return `${segundos} s`;
  };

  const diaActual = reloj.diaSimulado || 1;

  const titulos: Record<Seccion, { title: string; subtitle?: string }> = {
    registro: { title: 'Registro', subtitle: 'Gestiona la información de pedidos, avería y bloqueos para la simulación' },
    configuracion: { title: 'Configuración de la simulación', subtitle: 'Parámetros previos a ejecutar la simulación' },
    simulacion: { title: NOMBRE_ESCENARIO[escenarioSel] },
    reportes: { title: 'Reporte de la simulación', subtitle: 'Resumen de desempeño de la última corrida ejecutada' },
  };

  return (
    <div className="flex min-h-screen bg-surface-50 text-ink-900">
      <Sidebar activeSection={activeSection} onSelectSection={(s) => setActiveSection(s as Seccion)} />

      <div className="flex-1 flex flex-col min-w-0">
        <Header
          onFinalizarSimulacion={terminar}
          totalAlertas={0}
          isSimulating={motorActivo && !sinDatos}
          userName="Jones Ferdinand"
          title={titulos[activeSection]?.title ?? 'PAQRAP'}
          subtitle={titulos[activeSection]?.subtitle}
          showFinalizar={activeSection === 'simulacion' && !esOperacion}
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

        {/* Una sola pantalla para la Operación día a día y para las simulaciones (5D y colapso) */}
        {activeSection === 'simulacion' && (
          <main className="flex-1 p-6 md:p-8 flex flex-col w-full h-[calc(100vh-72px)] overflow-hidden">
            {/* Barra superior (sin tarjeta: va directo sobre el fondo, como el mockup) */}
            <div className="flex flex-wrap justify-between items-center gap-3 mb-4 px-1 text-sm">
              <div className="flex flex-wrap items-center gap-x-10 gap-y-2">
                <div className="text-ink-400 font-semibold">
                  Fecha: <span className="text-ink-900 font-bold ml-1">{sinDatos ? '--' : formatFecha(reloj.instanteActual)}</span>
                </div>

                <div className="flex items-center text-ink-400 font-semibold">
                  <ClockIcon className="w-4 h-4 mr-1.5" /> Tiempo transcurrido:
                  <span className="text-ink-900 font-bold ml-1">{sinDatos ? '--' : getTranscurrido()}</span>
                </div>
                <div className="text-ink-400 font-semibold">
                  Día: <span className="text-ink-900 font-bold ml-1">{diaActual}</span>
                </div>
                {/* La operación diaria corriendo se marca "En vivo"; cualquier escenario en pausa, "En pausa" */}
                {esOperacion && motorCorriendo && (
                  <span className="flex items-center gap-1.5 rounded-full bg-green-50 border border-green-200 px-2.5 py-0.5 text-xs font-bold text-green-700">
                    <span className="w-2 h-2 rounded-full bg-green-500 animate-pulse" aria-hidden="true" />
                    En vivo
                  </span>
                )}
                {estaPausada && !sinDatos && (
                  <span className="rounded-full bg-amber-50 border border-amber-200 px-2.5 py-0.5 text-xs font-bold text-amber-700">
                    En pausa
                  </span>
                )}
              </div>

              {/* Controles ▶ ⏸ ■ y velocidad: en la operación diaria y en las simulaciones */}
              {(
                <div className="flex items-center gap-2">
                  {sinDatos && (
                    <span className="text-xs font-semibold text-amber-600 mr-2">
                      {motorActivo
                        ? `El motor de simulación está ocupado con ${ocupadoPor ?? 'otro escenario'}, por eso aquí no se muestran datos.`
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
                    className={`${botonControl} ${estaPausada ? 'bg-brand-100' : ''}`}
                  >
                    <Pause size={16} fill="currentColor" />
                  </button>

                  {/* ■ Simulación: finaliza y lleva a Reportes · Operación diaria: solo la detiene */}
                  <button
                    onClick={esOperacion ? detenerOperacion : terminar}
                    disabled={!motorActivo}
                    title={esOperacion ? 'Detener la operación diaria' : 'Finalizar simulación'}
                    aria-label={esOperacion ? 'Detener la operación diaria' : 'Finalizar simulación'}
                    className={botonControl}
                  >
                    <Square size={16} fill="currentColor" />
                  </button>

                  <select
                    value={speed}
                    onChange={(e) => {
                      setSpeed(e.target.value);
                      // Multiplica la velocidad propia del escenario (por ejemplo, 2× la 5D = 300×)
                      simulacionApi.cambiarVelocidad(Number(e.target.value)).catch(() => {
                        /* si falla, la velocidad real se mantiene y el reloj lo refleja */
                      });
                    }}
                    aria-label="Velocidad de la simulación"
                    className="h-9 bg-white border border-brand-500 text-ink-900 rounded-md px-2 text-sm font-bold outline-none"
                  >
                    <option value="0.5">0.5×</option>
                    <option value="1">1.0×</option>
                    <option value="2">2.0×</option>
                  </select>
                </div>
              )}
            </div>

            {/* El mapa mantiene su alto. Al abrir el panel, el mapa se achica (ya no se esconde
                detrás del panel), así siempre se puede recorrer toda la ciudad. */}
            <div className="flex-1 min-h-0">
              <div className="relative h-full overflow-hidden rounded-xl">
                <div className="absolute inset-0 flex">
                  {/* Mapa: se achica cuando el panel está abierto (en pantallas grandes el panel es más ancho) */}
                  <div
                    className={`h-full shrink-0 transition-[width] duration-300 ease-in-out ${
                      panelAbierto ? 'w-[calc(100%-21rem)] 2xl:w-[calc(100%-24rem)]' : 'w-full'
                    }`}
                  >
                    <MapaOperaciones
                      almacenes={almacenes}
                      unidades={sinDatos ? [] : unidades}
                      pedidos={sinDatos ? [] : pedidos}
                      bloqueos={sinDatos ? [] : bloqueos}
                      rutas={sinDatos ? [] : rutasMapa}
                      selectedUnidadCodigo={selectedUnidadCodigo}
                      onSelectUnidad={setSelectedUnidadCodigo}
                      escenario={NOMBRE_ESCENARIO[escenarioSel]}
                      pausado={estaPausada}
                      reloj={{
                        instanteActual: reloj.instanteActual,
                        factorAceleracion: reloj.factorAceleracion,
                        corriendo: reloj.estadoEjecucion === 'EN_EJECUCION',
                      }}
                      mostrarPorAtender={esOperacion}
                      filtros={filtros}
                    />
                  </div>

                  {/* Panel derecho: 320 px; en pantallas grandes (≥ 1536 px) 368 px para que se lea mejor */}
                  <div className="w-80 2xl:w-[23rem] h-full shrink-0 ml-4" aria-hidden={!panelAbierto}>
                    <RightPanel
                      reloj={reloj}
                      almacenes={sinDatos ? [] : almacenes}
                      unidades={sinDatos ? [] : unidades}
                      pedidos={sinDatos ? [] : pedidos}
                      metricas={metricas}
                      selectedUnidadCodigo={selectedUnidadCodigo}
                      modo={esOperacion ? 'operacion' : 'simulacion'}
                      filtros={filtros}
                      onCambiarFiltros={setFiltros}
                    />
                  </div>
                </div>

                {/* Pestaña discreta: entre el mapa y el panel (abierto) o en el borde derecho (cerrado) */}
                <button
                  onClick={() => setPanelAbierto(!panelAbierto)}
                  title={panelAbierto ? 'Ocultar panel' : 'Mostrar panel'}
                  aria-label={panelAbierto ? 'Ocultar panel lateral' : 'Mostrar panel lateral'}
                  aria-expanded={panelAbierto}
                  className={`absolute top-1/2 -translate-y-1/2 z-30 w-4 h-14 flex items-center justify-center rounded-l-md bg-white border border-r-0 border-line-200 text-ink-400 hover:text-brand-500 shadow-sm transition-[right,color] duration-300 ease-in-out ${
                    panelAbierto ? 'right-80 2xl:right-[23rem]' : 'right-0'
                  }`}
                >
                  {panelAbierto ? <ChevronRight size={14} /> : <ChevronLeft size={14} />}
                </button>
              </div>
            </div>
          </main>
        )}

        {/* Ventana de emergencia de colapso logístico: modal rojo al centro, solo en la simulación de colapso */}
        <AlertaColapso
          metricas={metricas}
          detenidaPorColapso={reloj.estadoEjecucion === 'DETENIDA_POR_COLAPSO'}
          visible={activeSection === 'simulacion' && escenarioSel === 'COLAPSO_LOGISTICO'}
          onVerReporte={() => setActiveSection('reportes')}
        />
      </div>
    </div>
  );
};

const ClockIcon = ({ className }: { className?: string }) => (
  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className={className}><circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" /></svg>
);

export default App;
