import React from 'react';
import { SlidersHorizontal, Box, Lightbulb, ClipboardList } from 'lucide-react';

interface SidebarProps {
  activeSection?: string;
  onSelectSection?: (section: string) => void;
}

export const Sidebar: React.FC<SidebarProps> = ({
  activeSection = 'simulacion',
  onSelectSection,
}) => {
  return (
    <aside className="w-56 bg-white border-r border-gray-200 min-h-screen flex flex-col shrink-0 select-none">
      {/* Brand / Logo */}
      <div className="h-16 flex items-center px-6 gap-3 border-b border-gray-100">
        <div className="w-8 h-8 rounded-full bg-blue-600 flex items-center justify-center text-white font-bold shadow-sm shadow-blue-400/30">
          <svg
            className="w-4 h-4 fill-white"
            viewBox="0 0 24 24"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path d="M12 2L2 22h20L12 2zm0 4.5l6.5 13H5.5L12 6.5z" />
          </svg>
        </div>
        <span className="font-bold tracking-wider text-gray-700 text-lg">PAQRAP</span>
      </div>

      {/* Navigation items */}
      <nav className="p-4 space-y-1.5 flex-1">
        <button
          onClick={() => onSelectSection && onSelectSection('registro')}
          className={`w-full flex items-center gap-3 px-4 py-2.5 rounded-lg text-sm font-medium transition-colors ${
            activeSection === 'registro'
              ? 'bg-blue-50 text-blue-700 font-semibold'
              : 'text-gray-500 hover:text-gray-900 hover:bg-gray-50'
          }`}
        >
          <ClipboardList className="w-4 h-4" />
          <span>Registro</span>
        </button>

        <button
          onClick={() => onSelectSection && onSelectSection('configuracion')}
          className={`w-full flex items-center gap-3 px-4 py-2.5 rounded-lg text-sm font-medium transition-colors ${
            activeSection === 'configuracion'
              ? 'bg-blue-50 text-blue-700 font-semibold'
              : 'text-gray-500 hover:text-gray-900 hover:bg-gray-50'
          }`}
        >
          <SlidersHorizontal className="w-4 h-4" />
          <span>Configuración</span>
        </button>

        <button
          onClick={() => onSelectSection && onSelectSection('simulacion')}
          className={`w-full flex items-center gap-3 px-4 py-2.5 rounded-lg text-sm font-medium transition-colors ${
            activeSection === 'simulacion'
              ? 'bg-gray-100 text-gray-800 font-semibold shadow-xs'
              : 'text-gray-500 hover:text-gray-900 hover:bg-gray-50'
          }`}
        >
          <Box className="w-4 h-4 text-gray-600" />
          <span>Simulación 5D</span>
        </button>

        <button
          onClick={() => onSelectSection && onSelectSection('reportes')}
          className={`w-full flex items-center gap-3 px-4 py-2.5 rounded-lg text-sm font-medium transition-colors ${
            activeSection === 'reportes'
              ? 'bg-blue-50 text-blue-700 font-semibold'
              : 'text-gray-500 hover:text-gray-900 hover:bg-gray-50'
          }`}
        >
          <Lightbulb className="w-4 h-4" />
          <span>Reportes</span>
        </button>
      </nav>
    </aside>
  );
};
