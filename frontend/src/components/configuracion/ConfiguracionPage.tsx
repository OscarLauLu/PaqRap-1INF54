import React, { useEffect, useRef, useState } from 'react';
import { Car, Bike, Truck, CalendarDays, AlertTriangle, Info, Upload, Rocket, Loader2 } from 'lucide-react';
import { Card } from '../ui/Card';
import { simulacionApi } from '../../api/simulacionApi';
import { archivosApi } from '../../api/archivosApi';
import type { TipoEscenario } from '../../types';

interface Props {
  onSimulacionIniciada: (escenario: TipoEscenario) => void;
}

const ESCENARIOS: { id: TipoEscenario; titulo: string; detalle: string; Icono: React.ElementType }[] = [
  { id: 'DIA_A_DIA', titulo: 'Operación día a día', detalle: 'Operación en tiempo real de las entregas, sin duración fija.', Icono: Truck },
  { id: 'SIMULACION_5D', titulo: 'Simulación 5D', detalle: '5 días simulados de operación logística (30–60 min de ejecución real).', Icono: CalendarDays },
  { id: 'COLAPSO_LOGISTICO', titulo: 'Simulación de colapso', detalle: 'Corre hasta que un solo pedido incumpla su plazo límite.', Icono: AlertTriangle },
];

const spinnerBase =
  'w-full rounded-xl border border-(--color-ink-300) px-4 py-2.5 text-lg font-bold text-(--color-ink-900) text-center outline-none focus:border-(--color-brand-500) focus:ring-2 focus:ring-(--color-brand-100) transition-colors';

export const ConfiguracionPage: React.FC<Props> = ({ onSimulacionIniciada }) => {
  const [escenario, setEscenario] = useState<TipoEscenario>('DIA_A_DIA');
  const [numAutos, setNumAutos] = useState(10);
  const [numMotos, setNumMotos] = useState(10);
  const [numBicicletas, setNumBicicletas] = useState(10);

  const [archivoPedidos, setArchivoPedidos] = useState<{ nombre: string; ruta: string } | null>(null);
  const [archivoBloqueos, setArchivoBloqueos] = useState<{ nombre: string; ruta: string } | null>(null);
  const [subiendoPedidos, setSubiendoPedidos] = useState(false);
  const [subiendoBloqueos, setSubiendoBloqueos] = useState(false);
  const refPedidos = useRef<HTMLInputElement>(null);
  const refBloqueos = useRef<HTMLInputElement>(null);

  const [horasVerde, setHorasVerde] = useState(12);
  const [horasAmbar, setHorasAmbar] = useState(4);

  const [iniciando, setIniciando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    simulacionApi.obtenerSemaforo().then((s) => {
      setHorasVerde(s.horasVerde);
      setHorasAmbar(s.horasAmbar);
    });
  }, []);

  const subirArchivo = async (
    file: File,
    tipo: 'pedidos' | 'bloqueos',
    setEstado: (v: { nombre: string; ruta: string } | null) => void,
    setSubiendo: (v: boolean) => void,
  ) => {
    setSubiendo(true);
    try {
      const ruta = await archivosApi.subir(file, tipo);
      setEstado({ nombre: file.name, ruta });
    } catch {
      setError(`No se pudo subir el archivo de ${tipo}.`);
    } finally {
      setSubiendo(false);
    }
  };

  const iniciar = async () => {
    setIniciando(true);
    setError(null);
    try {
      if (horasAmbar >= horasVerde) {
        setError('El umbral Ámbar debe ser menor que el umbral Verde.');
        return;
      }
      await simulacionApi.actualizarSemaforo(horasVerde, horasAmbar);
      await simulacionApi.configurar({
        numAutos,
        numMotos,
        numBicicletas,
        archivoPedidos: archivoPedidos?.ruta,
        archivoBloqueos: archivoBloqueos?.ruta,
      });
      await simulacionApi.iniciarSimulacion(escenario);
      onSimulacionIniciada(escenario);
    } catch {
      setError('No se pudo iniciar la simulación. Verifica el backend e inténtalo de nuevo.');
    } finally {
      setIniciando(false);
    }
  };

  return (
    <main className="flex-1 p-6 md:p-8 space-y-6 max-w-5xl">
      <Card icono={Truck} titulo="Flota disponible">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {[
            { label: 'Autos', sub: '40 km/h · S/8.00/km · 24 paquetes', Icono: Car, value: numAutos, set: setNumAutos },
            { label: 'Motos', sub: '25 km/h · S/6.00/km · 8 paquetes', Icono: Truck, value: numMotos, set: setNumMotos },
            { label: 'Bicicletas', sub: '12 km/h · S/3.00/km · 4 paquetes', Icono: Bike, value: numBicicletas, set: setNumBicicletas },
          ].map(({ label, sub, Icono, value, set }) => (
            <div key={label} className="rounded-xl border border-(--color-line-200) p-4 flex flex-col items-center gap-2">
              <Icono className="w-7 h-7 text-(--color-brand-500)" />
              <span className="font-bold text-(--color-ink-900)">{label}</span>
              <span className="text-xs text-(--color-ink-400) text-center">{sub}</span>
              <input
                type="number"
                min={0}
                value={value}
                onChange={(e) => set(Math.max(0, Number(e.target.value)))}
                className={spinnerBase}
              />
            </div>
          ))}
        </div>
      </Card>

      <Card icono={Upload} titulo="Archivos de datos (opcional)">
        <p className="text-sm text-(--color-ink-500) mb-4">
          Si no subes archivos, la simulación usa los datos de ejemplo por defecto del servidor.
        </p>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <input ref={refPedidos} type="file" accept=".txt,.csv" className="hidden"
              onChange={(e) => { const f = e.target.files?.[0]; if (f) subirArchivo(f, 'pedidos', setArchivoPedidos, setSubiendoPedidos); e.target.value = ''; }} />
            <button type="button" onClick={() => refPedidos.current?.click()}
              className="w-full h-32 rounded-xl border-2 border-dashed border-(--color-brand-500) bg-(--color-brand-50) hover:bg-(--color-brand-100) transition-colors flex flex-col items-center justify-center gap-1 px-3">
              {subiendoPedidos ? <Loader2 className="w-7 h-7 text-(--color-brand-500) animate-spin" /> : <Upload className="w-7 h-7 text-(--color-brand-500)" />}
              <span className="font-bold text-(--color-brand-700) text-sm">Archivo de pedidos (mensual)</span>
              <span className="text-xs text-(--color-ink-400) truncate max-w-full">
                {archivoPedidos ? archivoPedidos.nombre : 'ningún archivo cargado'}
              </span>
            </button>
          </div>
          <div>
            <input ref={refBloqueos} type="file" accept=".txt,.csv" className="hidden"
              onChange={(e) => { const f = e.target.files?.[0]; if (f) subirArchivo(f, 'bloqueos', setArchivoBloqueos, setSubiendoBloqueos); e.target.value = ''; }} />
            <button type="button" onClick={() => refBloqueos.current?.click()}
              className="w-full h-32 rounded-xl border-2 border-dashed border-(--color-ink-300) bg-gray-50 hover:bg-gray-100 transition-colors flex flex-col items-center justify-center gap-1 px-3">
              {subiendoBloqueos ? <Loader2 className="w-7 h-7 text-(--color-ink-500) animate-spin" /> : <Upload className="w-7 h-7 text-(--color-ink-500)" />}
              <span className="font-bold text-(--color-ink-700) text-sm">Archivo de bloqueos</span>
              <span className="text-xs text-(--color-ink-400) truncate max-w-full">
                {archivoBloqueos ? archivoBloqueos.nombre : 'ningún archivo cargado'}
              </span>
            </button>
          </div>
        </div>
      </Card>

      <Card titulo="Selecciona el escenario">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {ESCENARIOS.map(({ id, titulo, detalle, Icono }) => {
            const activo = escenario === id;
            return (
              <button
                key={id}
                type="button"
                onClick={() => setEscenario(id)}
                className={`rounded-xl border-2 p-5 text-center flex flex-col items-center gap-2 transition-colors ${
                  activo ? 'border-(--color-brand-500) bg-(--color-brand-50)' : 'border-(--color-line-200) bg-white hover:bg-gray-50'
                }`}
              >
                <Icono className={`w-9 h-9 ${activo ? 'text-(--color-brand-500)' : 'text-(--color-ink-500)'}`} />
                <span className={`font-bold ${activo ? 'text-(--color-brand-700)' : 'text-(--color-ink-900)'}`}>{titulo}</span>
                <span className="text-xs text-(--color-ink-400)">{detalle}</span>
              </button>
            );
          })}
        </div>
      </Card>

      <Card icono={Info} titulo="Rangos del semáforo de criticidad">
        <p className="text-xs text-(--color-ink-400) mb-4">
          Valores vigentes en el backend (ConfiguracionSemaforo), en horas de holgura restante hasta el plazo límite. Cambiarlos aplica en caliente, sin reiniciar el sistema.
        </p>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3">
            <span className="block text-xs font-bold text-red-600 mb-1">Rojo — crítico</span>
            <span className="text-sm text-(--color-ink-700)">holgura &lt; {horasAmbar} h</span>
          </div>
          <div className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 flex items-center justify-between gap-2">
            <div>
              <span className="block text-xs font-bold text-amber-700 mb-1">Ámbar — en riesgo</span>
              <span className="text-sm text-(--color-ink-700)">{horasAmbar} h ≤ holgura &lt; </span>
            </div>
            <input type="number" min={0} value={horasAmbar} onChange={(e) => setHorasAmbar(Number(e.target.value))}
              className="w-16 rounded-lg border border-(--color-ink-300) px-2 py-1 text-center text-sm font-bold" />
          </div>
          <div className="rounded-lg border border-green-200 bg-green-50 px-4 py-3 flex items-center justify-between gap-2">
            <div>
              <span className="block text-xs font-bold text-green-700 mb-1">Verde — óptimo</span>
              <span className="text-sm text-(--color-ink-700)">holgura ≥ </span>
            </div>
            <input type="number" min={0} value={horasVerde} onChange={(e) => setHorasVerde(Number(e.target.value))}
              className="w-16 rounded-lg border border-(--color-ink-300) px-2 py-1 text-center text-sm font-bold" />
          </div>
        </div>
      </Card>

      {error && (
        <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-2.5 text-sm text-red-700">{error}</div>
      )}

      <div className="flex justify-end pb-4">
        <button
          type="button"
          onClick={iniciar}
          disabled={iniciando}
          className="flex items-center gap-2 rounded-xl bg-(--color-brand-500) hover:bg-(--color-brand-600) active:bg-(--color-brand-700) disabled:opacity-50 text-white font-bold px-8 py-3 text-sm transition-colors"
        >
          {iniciando ? <Loader2 className="w-4 h-4 animate-spin" /> : <Rocket className="w-4 h-4" />}
          Iniciar Simulación
        </button>
      </div>
    </main>
  );
};
