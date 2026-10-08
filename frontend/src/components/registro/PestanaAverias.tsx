import React, { useEffect, useState } from 'react';
import { Clock, FileText } from 'lucide-react';
import { averiasApi } from '../../api/averiasApi';
import type { TipoAveria } from '../../types/registro';
import { inputBase } from './estilos';
import { Aviso, Campo, Selector } from './ui';
import { formatFecha, formatFechaHora, formatHora, parseUbicacion } from './utils';

const TIPOS = [
  { valor: 'TIPO_1', etiqueta: 'Tipo 1 — 2 h en el lugar' },
  { valor: 'TIPO_2', etiqueta: 'Tipo 2 — hasta el próximo cambio de turno' },
  { valor: 'TIPO_3', etiqueta: 'Tipo 3 — ≥ 2 días' },
];

// Explicación client-side de la regla real (Averia.calcularReincorporacion, RF-14). El backend
// calcula la fecha exacta; esto solo informa al usuario antes de registrar.
const EXPLICACION_TIPO: Record<TipoAveria, string> = {
  TIPO_1: 'Permanece en el lugar 2 horas y vuelve a operar donde quedó.',
  TIPO_2: 'Permanece hasta el fin del siguiente turno (07:00, 15:00 o 23:00), máx. 4 h en el lugar, y se traslada de inmediato al almacén central.',
  TIPO_3: 'Permanece 4 h en el lugar, se traslada de inmediato al almacén central y reingresa a operar al menos 2 días después, en el turno 15:00–23:00.',
};

// Endpoint del reloj simulado (SimulacionController.consultarReloj)
const URL_RELOJ = '/api/simulacion/reloj';

interface Errores {
  idUnidad?: string;
  ubicacion?: string;
}

export const PestanaAverias: React.FC = () => {
  const [idUnidad, setIdUnidad] = useState('');
  const [ubicacion, setUbicacion] = useState('');
  const [tipo, setTipo] = useState<TipoAveria>('TIPO_1');
  const [errores, setErrores] = useState<Errores>({});
  const [mensaje, setMensaje] = useState<string | null>(null);
  const [registrando, setRegistrando] = useState(false);

  // Fecha y hora de la avería: solo se muestran (el backend las registra al guardar).
  // Si hay una simulación en ejecución se usa el reloj simulado; si no, la hora real.
  const [ahora, setAhora] = useState(() => new Date());
  const [esHoraSimulada, setEsHoraSimulada] = useState(false);
  useEffect(() => {
    let activo = true;
    const actualizar = async () => {
      try {
        const resp = await fetch(URL_RELOJ);
        if (!resp.ok) throw new Error();
        const reloj: { instanteActual?: string; estadoEjecucion?: string } = await resp.json();
        if (!activo) return;
        if (reloj.estadoEjecucion === 'EN_EJECUCION' && reloj.instanteActual) {
          setAhora(new Date(reloj.instanteActual));
          setEsHoraSimulada(true);
          return;
        }
      } catch {
        // Si no se puede consultar el reloj, se usa la hora real
      }
      if (!activo) return;
      setAhora(new Date());
      setEsHoraSimulada(false);
    };
    actualizar();
    const t = setInterval(actualizar, 2000);
    return () => {
      activo = false;
      clearInterval(t);
    };
  }, []);

  const limpiar = () => {
    setIdUnidad('');
    setUbicacion('');
    setTipo('TIPO_1');
    setErrores({});
  };

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    const ubic = parseUbicacion(ubicacion);
    const nuevos: Errores = {};
    if (!idUnidad.trim()) nuevos.idUnidad = 'Ingresa el código de la unidad';
    if (!ubic) nuevos.ubicacion = 'Usa el formato (x,y). Ej: (32,18)';

    setErrores(nuevos);
    if (!ubic || Object.keys(nuevos).length > 0) return;

    setRegistrando(true);
    try {
      const registrada = await averiasApi.registrar({
        idUnidad: idUnidad.trim().toUpperCase(),
        ubicacion: ubic,
        tipo,
      });

      setMensaje(
        registrada.horaReincorporacion
          ? `Avería ${registrada.codigo} registrada para ${registrada.idUnidad}. Reincorporación estimada: ${formatFechaHora(registrada.horaReincorporacion)}.`
          : `Avería ${registrada.codigo} registrada para ${registrada.idUnidad}.`,
      );
      limpiar();
    } catch {
      setMensaje('No se pudo registrar la avería. Verifica que el código de unidad exista.');
    } finally {
      setRegistrando(false);
    }
  };

  return (
    <>
      <div className="space-y-4">
        <form
          id="form-nueva-averia"
          onSubmit={handleSubmit}
          noValidate
          className="bg-white rounded-xl border border-(--color-line-200) shadow-sm p-6"
        >
          <div className="flex flex-wrap items-center justify-between gap-2 mb-4">
            <h2 className="flex items-center gap-2 text-lg font-bold text-(--color-ink-900)">
              <FileText className="w-5 h-5 text-(--color-brand-500)" />
              Nueva avería
            </h2>
            <span className="text-xs text-(--color-ink-400)">
              {esHoraSimulada ? 'Usando la hora de la simulación' : 'Usando la hora actual'}
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-x-6 gap-y-4">
            {/* Fila 1 */}
            <Campo etiqueta="ID Unidad" error={errores.idUnidad}>
              <input
                type="text"
                value={idUnidad}
                onChange={(e) => setIdUnidad(e.target.value)}
                placeholder="AUTO-03"
                className={inputBase}
              />
            </Campo>

            <Campo etiqueta="Ubicación" error={errores.ubicacion}>
              <input
                type="text"
                value={ubicacion}
                onChange={(e) => setUbicacion(e.target.value)}
                placeholder="(32,18)"
                className={inputBase}
              />
            </Campo>

            <Campo etiqueta="Fecha de avería">
              <input type="text" value={formatFecha(ahora)} readOnly className={`${inputBase} bg-gray-100`} />
            </Campo>

            <Campo etiqueta="Hora de avería">
              <div className="relative">
                <input type="text" value={formatHora(ahora)} readOnly className={`${inputBase} bg-gray-100 pr-10`} />
                <Clock className="w-5 h-5 text-gray-400 absolute right-3 top-1/2 -translate-y-1/2" />
              </div>
            </Campo>

            {/* Fila 2 */}
            <Campo etiqueta="Tipo de avería">
              <Selector value={tipo} onChange={(v) => setTipo(v as TipoAveria)} opciones={TIPOS} />
            </Campo>

            <Campo etiqueta="Fin de indisponibilidad">
              <div className="relative" title={EXPLICACION_TIPO[tipo]}>
                <input
                  type="text"
                  value=""
                  readOnly
                  placeholder="Se calcula al registrar"
                  className={`${inputBase} bg-gray-100 pr-10`}
                />
                <Clock className="w-5 h-5 text-gray-400 absolute right-3 top-1/2 -translate-y-1/2" />
              </div>
            </Campo>

            <Campo etiqueta="Estado">
              <input type="text" value="En reparación" readOnly className={`${inputBase} bg-gray-100`} />
            </Campo>
          </div>
        </form>

        {/* Botón de registro manual alineado a la derecha */}
        <div className="flex justify-end">
          <button
            type="submit"
            form="form-nueva-averia"
            disabled={registrando}
            className="rounded-xl bg-(--color-brand-500) hover:bg-(--color-brand-600) active:bg-(--color-brand-700) disabled:opacity-50 text-white font-semibold px-6 py-2.5 text-sm transition-colors"
          >
            {registrando ? 'Registrando...' : 'Registrar avería'}
          </button>
        </div>
      </div>

      {mensaje && <Aviso mensaje={mensaje} onCerrar={() => setMensaje(null)} />}
    </>
  );
};