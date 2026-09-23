import React, { useState } from 'react';
import { FileText, Info } from 'lucide-react';
import { averiasApi } from '../../api/averiasApi';
import type { TipoAveria } from '../../types/registro';
import { inputBase } from './estilos';
import { Aviso, Campo, Selector } from './ui';
import { formatFechaHora, parseUbicacion } from './utils';

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
      <form onSubmit={handleSubmit} noValidate className="bg-white rounded-xl border border-(--color-line-200) shadow-sm p-6">
        <h2 className="flex items-center gap-2 text-lg font-bold text-(--color-ink-900) mb-4">
          <FileText className="w-5 h-5 text-(--color-brand-500)" />
          Nueva avería
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-x-6 gap-y-4">
          <Campo etiqueta="ID Unidad" error={errores.idUnidad}>
            <input
              type="text"
              value={idUnidad}
              onChange={(e) => setIdUnidad(e.target.value)}
              placeholder="A1"
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

          <Campo etiqueta="Tipo de avería">
            <Selector value={tipo} onChange={(v) => setTipo(v as TipoAveria)} opciones={TIPOS} />
          </Campo>
        </div>

        <div className="mt-4 flex items-start gap-2 rounded-lg bg-(--color-brand-50) px-4 py-3 text-xs text-(--color-ink-500)">
          <Info className="w-4 h-4 text-(--color-brand-500) shrink-0 mt-0.5" />
          <p>
            <strong className="text-(--color-ink-700)">Fecha/hora del evento, reincorporación y estado los calcula el sistema</strong> a
            partir del tipo elegido (RF-14) — no se ingresan a mano. {EXPLICACION_TIPO[tipo]}
          </p>
        </div>

        <div className="mt-4 flex justify-end">
          <button
            type="submit"
            disabled={registrando}
            className="rounded-xl bg-(--color-brand-500) hover:bg-(--color-brand-600) active:bg-(--color-brand-700) disabled:opacity-50 text-white font-semibold px-6 py-2.5 text-sm transition-colors"
          >
            {registrando ? 'Registrando...' : 'Registrar avería'}
          </button>
        </div>
      </form>

      {mensaje && <Aviso mensaje={mensaje} onCerrar={() => setMensaje(null)} />}
    </>
  );
};
