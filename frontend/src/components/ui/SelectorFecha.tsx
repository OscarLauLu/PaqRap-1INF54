import React, { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { CalendarDays, ChevronLeft, ChevronRight } from 'lucide-react';

interface Props {
  /** Fecha en formato AAAA-MM-DD, o vacío si aún no se eligió */
  value: string;
  onChange: (valor: string) => void;
  ariaLabel?: string;
}

const MESES = [
  'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
  'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre',
];
const DIAS_SEMANA = ['L', 'M', 'M', 'J', 'V', 'S', 'D'];

const dos = (n: number) => String(n).padStart(2, '0');
const aIso = (anio: number, mes: number, dia: number) => `${anio}-${dos(mes)}-${dos(dia)}`;

/** AAAA-MM-DD → dd/mm/aaaa */
const formatear = (iso: string) => {
  if (!iso) return '';
  const [a, m, d] = iso.split('-');
  return `${d}/${m}/${a}`;
};

/** Texto escrito por el usuario (dd/mm/aaaa) → AAAA-MM-DD, o null si no es una fecha válida */
const interpretar = (texto: string): string | null => {
  const m = texto.trim().match(/^(\d{1,2})[\/.-](\d{1,2})[\/.-](\d{4})$/);
  if (!m) return null;
  const dia = Number(m[1]);
  const mes = Number(m[2]);
  const anio = Number(m[3]);
  const f = new Date(anio, mes - 1, dia);
  if (f.getFullYear() !== anio || f.getMonth() !== mes - 1 || f.getDate() !== dia) return null;
  return aIso(anio, mes, dia);
};

const campoClase =
  'w-full rounded-xl border border-ink-300 bg-white px-4 py-2.5 text-base text-ink-900 outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100 transition-colors placeholder:text-ink-400';

export const SelectorFecha: React.FC<Props> = ({ value, onChange, ariaLabel = 'Fecha' }) => {
  const hoy = new Date();
  const [abierto, setAbierto] = useState(false);
  const [texto, setTexto] = useState(formatear(value));
  const [borrador, setBorrador] = useState(value);
  const [vista, setVista] = useState({ anio: hoy.getFullYear(), mes: hoy.getMonth() });
  const [pos, setPos] = useState({ top: 0, left: 0, width: 0 });
  const cajaRef = useRef<HTMLDivElement>(null);
  const panelRef = useRef<HTMLDivElement>(null);

  // Referencias para que los escuchadores del documento siempre vean los valores actuales
  const valueRef = useRef(value);
  const textoRef = useRef(texto);
  const onChangeRef = useRef(onChange);
  const escritoRef = useRef(false);
  valueRef.current = value;
  textoRef.current = texto;
  onChangeRef.current = onChange;

  // Si la fecha cambia desde afuera (por ejemplo "Usar fecha y hora actual"), el texto la sigue
  useEffect(() => {
    setTexto(formatear(value));
  }, [value]);

  // Cierra sin guardar: el campo vuelve a la última fecha confirmada
  const descartar = () => {
    setTexto(formatear(valueRef.current));
    setAbierto(false);
  };

  // Cierra al hacer clic afuera: si el usuario escribió una fecha válida se conserva
  const cerrarAfuera = () => {
    if (escritoRef.current) {
      const iso = interpretar(textoRef.current);
      if (iso) {
        onChangeRef.current(iso);
        setAbierto(false);
        return;
      }
    }
    descartar();
  };

  const abrir = () => {
    if (abierto) return;
    const ref = value ? new Date(`${value}T00:00:00`) : new Date();
    setVista({ anio: ref.getFullYear(), mes: ref.getMonth() });
    setBorrador(value);
    escritoRef.current = false;
    const r = cajaRef.current?.getBoundingClientRect();
    if (r) setPos({ top: r.bottom + 8, left: r.left, width: r.width });
    setAbierto(true);
  };

  useEffect(() => {
    if (!abierto) return;
    const fuera = (e: MouseEvent) => {
      const t = e.target as Node;
      if (cajaRef.current?.contains(t) || panelRef.current?.contains(t)) return;
      cerrarAfuera();
    };
    const tecla = (e: KeyboardEvent) => {
      if (e.key === 'Escape') descartar();
    };
    const mover = () => descartar();
    document.addEventListener('mousedown', fuera);
    document.addEventListener('keydown', tecla);
    window.addEventListener('resize', mover);
    window.addEventListener('scroll', mover, true);
    return () => {
      document.removeEventListener('mousedown', fuera);
      document.removeEventListener('keydown', tecla);
      window.removeEventListener('resize', mover);
      window.removeEventListener('scroll', mover, true);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [abierto]);

  // Lo que se escribe se refleja de inmediato en el calendario
  const alEscribir = (v: string) => {
    setTexto(v);
    escritoRef.current = true;
    const iso = interpretar(v);
    if (iso) {
      setBorrador(iso);
      const [a, m] = iso.split('-').map(Number);
      setVista({ anio: a, mes: m - 1 });
    }
  };

  const confirmarTexto = () => {
    if (texto.trim() === '') {
      onChange('');
      return;
    }
    const iso = interpretar(texto);
    if (iso) onChange(iso);
    else setTexto(formatear(value)); // lo escrito no es una fecha válida: vuelve a la última válida
  };

  // Elegir un día lo muestra en el campo, pero solo se guarda con "Seleccionar"
  const elegirDia = (iso: string) => {
    setBorrador(iso);
    setTexto(formatear(iso));
    escritoRef.current = false;
  };

  const mesAnterior = () =>
    setVista((v) => (v.mes === 0 ? { anio: v.anio - 1, mes: 11 } : { anio: v.anio, mes: v.mes - 1 }));
  const mesSiguiente = () =>
    setVista((v) => (v.mes === 11 ? { anio: v.anio + 1, mes: 0 } : { anio: v.anio, mes: v.mes + 1 }));

  const anioBase = hoy.getFullYear();
  const anios: number[] = [];
  for (let a = Math.min(anioBase - 2, vista.anio); a <= Math.max(anioBase + 4, vista.anio); a++) anios.push(a);

  // La semana empieza en lunes
  const huecos = (new Date(vista.anio, vista.mes, 1).getDay() + 6) % 7;
  const diasDelMes = new Date(vista.anio, vista.mes + 1, 0).getDate();
  const celdas: (number | null)[] = [
    ...Array.from({ length: huecos }, () => null),
    ...Array.from({ length: diasDelMes }, (_, i) => i + 1),
  ];
  // Siempre 6 filas, para que el calendario mida lo mismo en cualquier mes
  while (celdas.length < 42) celdas.push(null);
  const isoHoy = aIso(hoy.getFullYear(), hoy.getMonth() + 1, hoy.getDate());

  return (
    <>
      <div ref={cajaRef} className="relative group">
        <input
          type="text"
          inputMode="numeric"
          placeholder="dd/mm/aaaa"
          aria-label={ariaLabel}
          value={texto}
          onChange={(e) => alEscribir(e.target.value)}
          onFocus={abrir}
          onClick={abrir}
          onBlur={() => {
            if (!abierto) confirmarTexto();
          }}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              confirmarTexto();
              setAbierto(false);
            }
          }}
          className={`${campoClase} pl-11`}
        />
        <button
          type="button"
          onClick={() => (abierto ? descartar() : abrir())}
          aria-label="Abrir calendario"
          aria-expanded={abierto}
          className={`absolute inset-y-0 left-0 flex items-center px-3.5 transition-colors group-focus-within:text-brand-500 hover:text-brand-500 ${abierto ? 'text-brand-500' : 'text-ink-400'}`}
        >
          <CalendarDays className="w-5 h-5" />
        </button>
      </div>

      {abierto &&
        createPortal(
          <div
            ref={panelRef}
            role="dialog"
            aria-label="Calendario"
            style={{ position: 'fixed', top: pos.top, left: pos.left, width: pos.width }}
            className="z-50 flex h-[19rem] flex-col rounded-xl border border-gray-200 bg-white p-2.5 shadow-xl"
          >
            <div className="flex items-center gap-1.5 mb-1.5">
              <button
                type="button"
                onClick={mesAnterior}
                aria-label="Mes anterior"
                className="shrink-0 rounded-md border border-gray-300 p-1.5 text-ink-700 hover:bg-gray-50"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>
              <label className="flex-1 min-w-0">
                <select
                  aria-label="Mes"
                  value={vista.mes}
                  onChange={(e) => setVista({ ...vista, mes: Number(e.target.value) })}
                  className="w-full rounded-md border border-gray-300 bg-white px-2 py-1 text-sm text-ink-900 outline-none focus:border-brand-500"
                >
                  {MESES.map((nombre, k) => (
                    <option key={nombre} value={k}>{nombre}</option>
                  ))}
                </select>
              </label>
              <label className="w-[5.5rem] shrink-0">
                <select
                  aria-label="Año"
                  value={vista.anio}
                  onChange={(e) => setVista({ ...vista, anio: Number(e.target.value) })}
                  className="w-full rounded-md border border-gray-300 bg-white px-2 py-1 text-sm text-ink-900 outline-none focus:border-brand-500"
                >
                  {anios.map((a) => (
                    <option key={a} value={a}>{a}</option>
                  ))}
                </select>
              </label>
              <button
                type="button"
                onClick={mesSiguiente}
                aria-label="Mes siguiente"
                className="shrink-0 rounded-md border border-gray-300 p-1.5 text-ink-700 hover:bg-gray-50"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>

            <div className="grid grid-cols-7 text-center text-xs font-bold text-ink-700">
              {DIAS_SEMANA.map((d, k) => (
                <span key={k} className="py-0.5">{d}</span>
              ))}
            </div>

            <div className="grid grid-cols-7 gap-y-0.5 text-center">
              {celdas.map((dia, k) => {
                if (dia === null) return <span key={`h${k}`} />;
                const iso = aIso(vista.anio, vista.mes + 1, dia);
                const elegido = borrador === iso;
                const esHoy = iso === isoHoy;
                return (
                  <button
                    key={iso}
                    type="button"
                    onClick={() => elegirDia(iso)}
                    onDoubleClick={() => {
                      onChange(iso);
                      setAbierto(false);
                    }}
                    aria-pressed={elegido}
                    className={`mx-auto h-7 w-7 rounded-md text-sm transition-colors ${
                      elegido
                        ? 'bg-brand-500 text-white font-bold'
                        : esHoy
                          ? 'border border-brand-500 text-brand-600 font-semibold hover:bg-brand-50'
                          : 'text-ink-700 hover:bg-gray-100'
                    }`}
                  >
                    {dia}
                  </button>
                );
              })}
            </div>

            <div className="flex items-center justify-center gap-3 mt-2 pt-2 border-t border-gray-100">
              <button
                type="button"
                disabled={!borrador}
                onClick={() => {
                  if (borrador) onChange(borrador);
                  setAbierto(false);
                }}
                className="rounded-md bg-brand-500 hover:bg-brand-600 disabled:opacity-50 text-white font-semibold px-5 py-1 text-sm transition-colors"
              >
                Seleccionar
              </button>
              <button
                type="button"
                onClick={descartar}
                className="rounded-md border border-gray-300 bg-white hover:bg-gray-50 text-ink-700 font-semibold px-5 py-1 text-sm transition-colors"
              >
                Cancelar
              </button>
            </div>
          </div>,
          document.body,
        )}
    </>
  );
};
