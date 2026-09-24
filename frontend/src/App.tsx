import React, { useState } from 'react';
import { Sidebar } from './components/layout/Sidebar';
import { Header } from './components/layout/Header';
import { MapaOperaciones } from './components/mapa/MapaOperaciones';
import { RightPanel } from './components/panel/RightPanel';
import { RegistroPage } from './components/registro/RegistroPage';
import { ConfiguracionPage } from './components/configuracion/ConfiguracionPage';
import { OperacionDiariaPage } from './components/operacion/OperacionDiariaPage';
import { PantallaPendiente } from './components/simulacion/PantallaPendiente';
import { ReportePage } from './components/reportes/ReportePage';
import { Play, Pause, Square } from 'lucide-react';

import { useAlmacenes } from './hooks/useAlmacenes';
import { useSimulacion } from './hooks/useSimulacion';
import { useFlota } from './hooks/useFlota';
import { useMapaOperaciones } from './hooks/useMapaOperaciones';
import { useMetricas } from './hooks/useMetricas';
import { usePersistente } from './hooks/usePersistente';
import { NOMBRE_ESCENARIO, esEscenario } from './types/escenarios';
import type { Escenario } from './types/escenarios';
import type { RelojSimuladoData } from './types';

type Seccion = 'registro' | 'configuracion' | 'simulacion' | 'reportes';

export const App: React.FC = () => {
  const [activeSection, setActiveSection] = useState<Seccion>('configuracion');
  const [speed, setSpeed] = useState('1');

  const { almacenes } = useAlmacenes(5000);
  const { reloj, iniciar, detener, finalizarSimulacion } = useSimulacion(3000);
  const { unidades, selectedUnidadCodigo, setSelectedUnidadCodigo } = useFlota(4000);
  const { pedidos } = useMapaOperaciones(4000);
  const { metricas } = useMetricas(5000);

  // Configuración elegida (se recuerda al recargar la página)
  const [escenarioSel, setEscenarioSel] = usePersistente<Escenario>('paqrap.escenarioSel', 'DIA_A_DIA', esEscenario);
  const [fechaInicio, setFechaInicio] = usePersistente<string>('paqrap.fechaInicio', '2026-08-11');
  const [horaInicio, setHoraInicio] = usePersistente<string>('paqrap.horaInicio', '06:00');

  // El backend tiene UN solo motor y sus datos (vehículos, pedidos, stock) pertenecen
  // al ÚLTIMO escenario que lo inició, aunque ya haya terminado o se haya detenido.
  // Recordamos cuál fue (el backend todavía no lo informa en /api/simulacion/reloj).
  const [escenarioMotor, setEscenarioMotor] = usePersistente<Escenario | null>(
    'paqrap.escenarioMotor',
    null,
    (v) => v === null || esEscenario(v),
  );
  
  const motorCorriendo = reloj.estadoEjecucion === 'EN_EJECUCION';
  
  // Si el backend informa el escenario (campo "escenario" de /api/simulacion/reloj) se usa ese;
  // si no, se usa el que recordó este navegador.
  const escenarioBackend = (reloj as RelojSimuladoData & { escenario?: string | null }).escenario;
  const duenoDatos: Escenario | null = esEscenario(escenarioBackend) ? escenarioBackend : escenarioMotor;
  
  // Una pantalla solo muestra datos si son de SU escenario. Si el motor corre y no se sabe
  // quién lo inició (p. ej. se limpió el navegador), tampoco se muestran.
  const sinDatos = (duenoDatos !== null && duenoDatos !== escenarioSel) || (motorCorriendo && duenoDatos === null);
  const ocupadoPor = duenoDatos ? NOMBRE_ESCENARIO[duenoDatos] : null;

  const arrancar = async (escenario: Escenario) => {
    setEscenarioMotor(escenario);
    try {
      await iniciar(escenario);
    } catch {
      // El motor puede tardar en responder; el reloj se sigue consultando solo.
    }
  };

  const pausar = async () => {
    try {
      await detener();
    } catch {
      // Si falla, el estado real lo trae el reloj en la próxima consulta.
    }
  };

  const terminar = async () => {
    await finalizarSimulacion();
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

  const getTranscurrido = () => {
    if (!reloj.instanteActual) return '0 h';
    const hours = (reloj.diaSimulado - 1) * 24 + parseInt(reloj.horaSimulada.split(':')[0] || '0');
    return `${hours} h`;
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
          isSimulating={motorCorriendo && !sinDatos}
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
              onDetener={pausar}
              onFinalizar={terminar}
            />
          ) : escenarioSel === 'COLAPSO_LOGISTICO' ? (
            <PantallaPendiente titulo="Simulación de colapso" detalle="La pantalla de este escenario todavía no está desarrollada." />
          ) : (
            <main className="flex-1 p-6 md:p-8 flex flex-col w-full h-[calc(100vh-72px)] overflow-hidden">
              {/* Top Bar - Controls */}
              <div className="flex justify-between items-center mb-4 text-sm font-semibold text-(--color-ink-500) bg-white p-3 rounded-xl border border-(--color-line-200) shadow-sm">
                <div className="flex items-center space-x-8 px-4">
                  <div>Fecha: <span className="text-(--color-ink-900) ml-1">{sinDatos ? '--' : formatFecha(reloj.instanteActual)}</span></div>
                  <div className="flex items-center"><ClockIcon className="w-4 h-4 mr-1" /> Tiempo transcurrido: <span className="text-(--color-ink-900) ml-1">{sinDatos ? '--' : getTranscurrido()}</span></div>
                  <div>Día: <span className="text-(--color-ink-900) ml-1">{diaActual}</span></div>
                </div>

                <div className="flex items-center space-x-2">
                  {sinDatos && (
                    <span className="text-xs font-semibold text-amber-600 mr-2">
                      {motorCorriendo
                        ? `${ocupadoPor ?? 'Otro escenario'} en ejecución: presiona ■ para detenerlo`
                        : `Estos datos son de ${ocupadoPor}: presiona ▶ para iniciar la 5D`}
                    </span>
                  )}
                  <button onClick={() => arrancar('SIMULACION_5D')} disabled={motorCorriendo} className="p-2 bg-(--color-brand-50) text-(--color-brand-500) rounded-lg border border-(--color-brand-100) hover:bg-(--color-brand-100) transition-colors disabled:opacity-40 disabled:cursor-not-allowed"><Play size={18} fill="currentColor" /></button>
                  <button onClick={pausar} className="p-2 bg-gray-50 text-(--color-ink-500) rounded-lg border border-gray-200 hover:bg-gray-100 transition-colors"><Pause size={18} fill="currentColor" /></button>
                  <button onClick={terminar} className="p-2 bg-gray-50 text-(--color-ink-500) rounded-lg border border-gray-200 hover:bg-gray-100 transition-colors"><Square size={18} fill="currentColor" /></button>
                  <select value={speed} onChange={(e) => setSpeed(e.target.value)} className="bg-white border border-gray-300 text-(--color-ink-700) rounded-lg p-1.5 text-sm font-bold ml-2 outline-none">
                    <option value="0.5">0.5x</option>
                    <option value="1">1.0x</option>
                    <option value="2">2.0x</option>
                  </select>
                </div>
              </div>

              <div className="flex flex-1 gap-6 min-h-0">
                {/* Mapa (Flex-1) */}
                <div className="flex-1 h-full flex flex-col">
                  <MapaOperaciones
                    almacenes={almacenes}
                    unidades={sinDatos ? [] : unidades}
                    pedidos={sinDatos ? [] : pedidos}
                    selectedUnidadCodigo={selectedUnidadCodigo}
                    onSelectUnidad={setSelectedUnidadCodigo}
                  />
                </div>

                {/* Right Panel (Fixed Width) */}
                <RightPanel
                  reloj={reloj}
                  almacenes={sinDatos ? [] : almacenes}
                  unidades={sinDatos ? [] : unidades}
                  pedidos={sinDatos ? [] : pedidos}
                  metricas={metricas}
                  selectedUnidadCodigo={selectedUnidadCodigo}
                />
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
