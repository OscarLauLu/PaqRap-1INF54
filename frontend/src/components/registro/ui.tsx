import React from 'react';
import { ChevronDown } from 'lucide-react';
import { inputBase } from './estilos';

export const Campo: React.FC<{ etiqueta: string; error?: string; children: React.ReactNode }> = ({
  etiqueta,
  error,
  children,
}) => (
  <div>
    <label className="block text-sm font-semibold text-gray-700 mb-1.5">{etiqueta}</label>
    {children}
    {error && <p className="text-xs text-red-500 mt-1">{error}</p>}
  </div>
);

interface SelectorProps {
  value: string;
  onChange: (valor: string) => void;
  opciones: { valor: string; etiqueta: string }[];
}

export const Selector: React.FC<SelectorProps> = ({ value, onChange, opciones }) => (
  <div className="relative">
    <select
      value={value}
      onChange={(e) => onChange(e.target.value)}
      className={`${inputBase} appearance-none bg-white pr-10`}
    >
      {opciones.map((o) => (
        <option key={o.valor} value={o.valor}>
          {o.etiqueta}
        </option>
      ))}
    </select>
    <ChevronDown className="w-4 h-4 text-blue-500 absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none" />
  </div>
);

export const Aviso: React.FC<{ mensaje: string; onCerrar: () => void }> = ({ mensaje, onCerrar }) => (
  <div className="rounded-lg border border-blue-200 bg-blue-50 px-4 py-2.5 text-sm text-blue-700 flex justify-between gap-4">
    <span>{mensaje}</span>
    <button type="button" onClick={onCerrar} className="font-bold" aria-label="Cerrar">
      ×
    </button>
  </div>
);
