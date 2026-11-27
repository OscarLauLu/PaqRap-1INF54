import React, { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { Check, Clock, X } from 'lucide-react';

interface Props {
  /** Hora en formato 24 h "HH:MM", o vacío si aún no se eligió */
  value: string;
  onChange: (valor: string) => void;
  ariaLabel?: string;
}

const dos = (n: number) => String(n).padStart(2, '0');
const HORAS = Array.from({ length: 12 }, (_, i) => i + 1);
const MINUTOS = Array.from({ length: 12 }, (_, i) => i * 5);

/** "HH:MM" (24 h) → { h12, min, pm } */
const separar = (v: string) => {
  const [h, m] = v.split(':').map(Number);
  return { h12: h % 12 === 0 ? 12 : h % 12, min: m, pm: h >= 12 };
};
/** { h12, min, pm } → "HH:MM" (24 h) */
const unir = (h12: number, min: number, pm: boolean) => `${dos((h12 % 12) + (pm ? 12 : 0))}:${dos(min)}`;

/** "HH:MM" → "hh:mm AM/PM" */
const formatear = (v: string) => {
  if (!v) return '';
  const { h12, min, pm } = separar(v);
  return `${dos(h12)}:${dos(min)} ${pm ? 'PM' : 'AM'}`;
};

/** Texto escrito: "8:15 pm", "08:15PM", "20:15"… → "HH:MM" o null si no es válido */
const interpretar = (texto: string): string | null => {
  const m = texto.trim().toLowerCase().match(/^(\d{1,2})[:.](\d{2})\s*(a\.?m\.?|p\.?m\.?)?$/);
  if (!m) return null;
  let h = Number(m[1]);
  const min = Number(m[2]);
  if (min > 59) return null;
  if (m[3]) {
    if (h < 1 || h > 12) return null;
    const pm = m[3].startsWith('p');
    h = (h % 12) + (pm ? 12 : 0);
  } else if (h > 23) return null;
  return `${dos(h)}:${dos(min)}`;
};

const campoClase =
  'w-full rounded-xl border border-ink-300 bg-white px-4 py-2.5 text-base text-ink-900 outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100 transition-colors placeholder:text-ink-400';

const Columna: React.FC<{
  items: { clave: string; etiqueta: string; activo: boolean; elegir: () => void }[];
  ancho?: string;
}> = ({ items, ancho = 'flex-1' }) => {
  const ref = useRef<HTMLDivElement>(null);
  // Al abrir, la opción elegida queda visible en el centro de la columna
  useEffect(() => {
    const el = ref.current?.querySelector<HTMLElement>('[data-activo="true"]');
    if (el && ref.current) ref.current.scrollTop = el.offsetTop - ref.current.clientHeight / 2 + el.clientHeight / 2;
  }, []);
  return (
    <div ref={ref} className={`${ancho} h-full overflow-y-auto px-1 py-1 space-y-0.5 [scrollbar-width:thin]`}>
      {items.map((it) => (
        <button
          key={it.clave}
          type="button"
          data-activo={it.activo}
          onClick={it.elegir}
          className={`block w-full rounded-md py-2 text-sm transition-colors ${
            it.activo ? 'bg-brand-500 text-white font-bold' : 'text-ink-700 hover:bg-gray-100'
          }`}
        >
          {it.etiqueta}
        </button>
      ))}
    </div>
  );
};

export const SelectorHora: React.FC<Props> = ({ value, onChange, ariaLabel = 'Hora' }) => {
  const [abierto, setAbierto] = useState(false);
  const [texto, setTexto] = useState(formatear(value));
  const [borrador, setBorrador] = useState({ h12: 8, min: 0, pm: false });
  const [pos, setPos] = useState({ top: 0, left: 0, width: 0 });
  const cajaRef = useRef<HTMLDivElement>(null);
  const panelRef = useRef<HTMLDivElement>(null);

  const valueRef = useRef(value);
  const textoRef = useRef(texto);
  const onChangeRef = useRef(onChange);
  const escritoRef = useRef(false);
  valueRef.current = value;
  textoRef.current = texto;
  onChangeRef.current = onChange;

  useEffect(() => {
    setTexto(formatear(value));
  }, [value]);

  // Cierra sin guardar: el campo vuelve a la última hora confirmada
  const descartar = () => {
    setTexto(formatear(valueRef.current));
    setAbierto(false);
  };

  // Cierra al hacer clic afuera: si el usuario escribió una hora válida se conserva
  const cerrarAfuera = () => {
    if (escritoRef.current) {
      const v = interpretar(textoRef.current);
      if (v) {
        onChangeRef.current(v);
        setAbierto(false);
        return;
      }
    }
    descartar();
  };

  const abrir = () => {
    if (abierto) return;
    if (value) {
      const s = separar(value);
      setBorrador({ ...s, min: Math.min(55, Math.round(s.min / 5) * 5) });
    } else {
      setBorrador({ h12: 8, min: 0, pm: false });
    }
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
    const mover = (e: Event) => {
      // El desplazamiento dentro de las columnas no debe cerrar el panel
      if (e.target instanceof Node && panelRef.current?.contains(e.target)) return;
      descartar();
    };
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

  // Elegir en las columnas lo muestra en el campo, pero solo se guarda con ✓
  const elegir = (nuevo: { h12: number; min: number; pm: boolean }) => {
    setBorrador(nuevo);
    setTexto(formatear(unir(nuevo.h12, nuevo.min, nuevo.pm)));
    escritoRef.current = false;
  };

  const alEscribir = (v: string) => {
    setTexto(v);
    escritoRef.current = true;
    const h = interpretar(v);
    if (h) setBorrador(separar(h));
  };

  const confirmarTexto = () => {
    if (texto.trim() === '') {
      onChange('');
      return;
    }
    const v = interpretar(texto);
    if (v) onChange(v);
    else setTexto(formatear(value));
  };

  return (
    <>
      <div ref={cajaRef} className="relative group">
        <input
          type="text"
          placeholder="hh:mm AM"
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
          aria-label="Abrir selector de hora"
          aria-expanded={abierto}
          className={`absolute inset-y-0 left-0 flex items-center px-3.5 transition-colors group-focus-within:text-brand-500 hover:text-brand-500 ${abierto ? 'text-brand-500' : 'text-ink-400'}`}
        >
          <Clock className="w-5 h-5" />
        </button>
      </div>

      {abierto &&
        createPortal(
          <div
            ref={panelRef}
            role="dialog"
            aria-label="Selector de hora"
            style={{ position: 'fixed', top: pos.top, left: pos.left, width: pos.width }}
            className="z-50 flex h-[19rem] flex-col rounded-xl border border-gray-200 bg-white shadow-xl overflow-hidden"
          >
            <div className="flex min-h-0 flex-1 divide-x divide-gray-100 px-1 pt-1">
              <Columna
                items={HORAS.map((h) => ({
                  clave: `h${h}`,
                  etiqueta: String(h),
                  activo: borrador.h12 === h,
                  elegir: () => elegir({ ...borrador, h12: h }),
                }))}
              />
              <Columna
                items={MINUTOS.map((m) => ({
                  clave: `m${m}`,
                  etiqueta: dos(m),
                  activo: borrador.min === m,
                  elegir: () => elegir({ ...borrador, min: m }),
                }))}
              />
              <Columna
                ancho="w-16 shrink-0"
                items={[
                  { clave: 'am', etiqueta: 'AM', activo: !borrador.pm, elegir: () => elegir({ ...borrador, pm: false }) },
                  { clave: 'pm', etiqueta: 'PM', activo: borrador.pm, elegir: () => elegir({ ...borrador, pm: true }) },
                ]}
              />
            </div>
            <div className="grid grid-cols-2 border-t border-gray-100">
              <button
                type="button"
                onClick={() => {
                  onChange(unir(borrador.h12, borrador.min, borrador.pm));
                  setAbierto(false);
                }}
                aria-label="Aceptar"
                className="flex justify-center py-2.5 text-brand-600 hover:bg-gray-50"
              >
                <Check className="w-5 h-5" />
              </button>
              <button
                type="button"
                onClick={descartar}
                aria-label="Cancelar"
                className="flex justify-center py-2.5 text-ink-700 hover:bg-gray-50"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
          </div>,
          document.body,
        )}
    </>
  );
};
