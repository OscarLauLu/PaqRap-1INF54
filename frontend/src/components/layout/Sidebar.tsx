import React from 'react';
import { SlidersHorizontal, Box, Lightbulb, ClipboardList, ChevronsLeft, ChevronsRight } from 'lucide-react';
import { usePersistente } from '../../hooks/usePersistente';

interface SidebarProps {
  activeSection?: string;
  onSelectSection?: (section: string) => void;
}

const SECCIONES: { id: string; nombre: string; Icono: React.ElementType }[] = [
  { id: 'registro', nombre: 'Registro', Icono: ClipboardList },
  { id: 'configuracion', nombre: 'Configuración', Icono: SlidersHorizontal },
  { id: 'simulacion', nombre: 'Simulación', Icono: Box },
  { id: 'reportes', nombre: 'Reportes', Icono: Lightbulb },
];

export const Sidebar: React.FC<SidebarProps> = ({
  activeSection = 'configuracion',
  onSelectSection,
}) => {
  // Se recuerda al recargar la página
  const [contraido, setContraido] = usePersistente<boolean>(
    'paqrap.menuContraido',
    false,
    (v) => typeof v === 'boolean',
  );

  return (
    <aside
      className={`${
        contraido ? 'w-16' : 'w-56'
      } bg-white border-r border-gray-200 min-h-screen flex flex-col shrink-0 select-none transition-[width] duration-200 ease-in-out overflow-hidden`}
    >
      {/* Brand / Logo */}
      <div
        className={`h-16 flex items-center gap-3 border-b border-gray-100 ${
          contraido ? 'justify-center px-0' : 'px-6'
        }`}
      >
        <div className="w-8 h-8 shrink-0 rounded-full bg-(--color-brand-500) flex items-center justify-center text-white font-bold shadow-sm shadow-blue-400/30">
          <svg className="w-4 h-4 fill-white" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 2L2 22h20L12 2zm0 4.5l6.5 13H5.5L12 6.5z" />
          </svg>
        </div>
        {!contraido && (
          <span className="font-bold tracking-wider text-gray-700 text-lg whitespace-nowrap">PAQRAP</span>
        )}
      </div>

      {/* Navigation items */}
      <nav className={`space-y-1.5 flex-1 ${contraido ? 'p-2' : 'p-4'}`}>
        {SECCIONES.map(({ id, nombre, Icono }) => {
          const activa = activeSection === id;
          return (
            <button
              key={id}
              onClick={() => onSelectSection && onSelectSection(id)}
              title={contraido ? nombre : undefined}
              aria-label={nombre}
              aria-current={activa ? 'page' : undefined}
              className={`w-full flex items-center rounded-lg text-sm font-medium transition-colors ${
                contraido ? 'justify-center px-0 py-2.5' : 'gap-3 px-4 py-2.5'
              } ${
                activa
                  ? 'bg-blue-50 text-blue-700 font-semibold'
                  : 'text-gray-500 hover:text-gray-900 hover:bg-gray-50'
              }`}
            >
              <Icono className={contraido ? 'w-5 h-5' : 'w-4 h-4'} />
              {!contraido && <span className="whitespace-nowrap">{nombre}</span>}
            </button>
          );
        })}
      </nav>

      {/* Botón para contraer / expandir el menú */}
      <div className={`border-t border-gray-100 ${contraido ? 'p-2' : 'p-4'}`}>
        <button
          onClick={() => setContraido(!contraido)}
          title={contraido ? 'Expandir menú' : 'Contraer menú'}
          aria-label={contraido ? 'Expandir menú' : 'Contraer menú'}
          aria-expanded={!contraido}
          className={`w-full flex items-center rounded-lg text-sm font-medium text-gray-500 hover:text-gray-900 hover:bg-gray-50 transition-colors py-2.5 ${
            contraido ? 'justify-center px-0' : 'gap-3 px-4'
          }`}
        >
          {contraido ? <ChevronsRight className="w-5 h-5" /> : <ChevronsLeft className="w-4 h-4" />}
          {!contraido && <span className="whitespace-nowrap">Contraer menú</span>}
        </button>
      </div>
    </aside>
  );
};
