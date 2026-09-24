import React from 'react';

interface CardProps {
  icono?: React.ElementType;
  titulo?: string;
  accion?: React.ReactNode;
  children: React.ReactNode;
  className?: string;
}

/** Tarjeta base estandarizada (borde gris, esquinas 12px, sombra suave) usada en Registro,
 * Configuración y Reportes para que las pantallas compartan el mismo lenguaje visual. */
export const Card: React.FC<CardProps> = ({ icono: Icono, titulo, accion, children, className = '' }) => (
  <section className={`bg-white rounded-xl border border-(--color-line-200) shadow-sm p-6 ${className}`}>
    {(titulo || accion) && (
      <div className="flex items-center justify-between mb-4">
        {titulo && (
          <h2 className="flex items-center gap-2 text-lg font-bold text-(--color-ink-900)">
            {Icono && <Icono className="w-5 h-5 text-(--color-brand-500)" />}
            {titulo}
          </h2>
        )}
        {accion}
      </div>
    )}
    {children}
  </section>
);

export const StatTile: React.FC<{
  etiqueta: string;
  valor: React.ReactNode;
  tono?: 'neutro' | 'verde' | 'ambar' | 'rojo' | 'azul';
  icono?: React.ElementType;
}> = ({ etiqueta, valor, tono = 'neutro', icono: Icono }) => {
  const colores: Record<string, string> = {
    neutro: 'text-(--color-ink-900)',
    verde: 'text-(--color-semaforo-verde)',
    ambar: 'text-[#b8860b]',
    rojo: 'text-(--color-semaforo-rojo)',
    azul: 'text-(--color-brand-500)',
  };
  return (
    <div className="rounded-xl border border-(--color-line-200) bg-white px-5 py-4 flex flex-col gap-1">
      <span className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-(--color-ink-400)">
        {Icono && <Icono className="w-3.5 h-3.5" />}
        {etiqueta}
      </span>
      <span className={`text-2xl font-extrabold ${colores[tono]}`}>{valor}</span>
    </div>
  );
};

export const EmptyState: React.FC<{ icono: React.ElementType; titulo: string; detalle?: string }> = ({
  icono: Icono,
  titulo,
  detalle,
}) => (
  <div className="flex flex-col items-center justify-center gap-2 py-10 text-center text-(--color-ink-400)">
    <Icono className="w-8 h-8" />
    <p className="font-semibold text-(--color-ink-700)">{titulo}</p>
    {detalle && <p className="text-xs max-w-xs">{detalle}</p>}
  </div>
);
