import React from 'react';
import { Search, Bell } from 'lucide-react';

interface HeaderProps {
  onFinalizarSimulacion: () => void;
  totalAlertas?: number;
  userName?: string;
  isSimulating?: boolean;
}

export const Header: React.FC<HeaderProps> = ({
  onFinalizarSimulacion,
  totalAlertas = 0,
  userName = 'Jones Ferdinand',
  isSimulating = true,
}) => {
  return (
    <header className="h-16 bg-white border-b border-gray-200 px-8 flex items-center justify-between">
      {/* Title */}
      <h1 className="text-xl font-bold text-gray-800 tracking-tight">Simulación 5D</h1>

      {/* Right controls */}
      <div className="flex items-center gap-5">
        {/* Red action button */}
        <button
          onClick={onFinalizarSimulacion}
          className="bg-red-500 hover:bg-red-600 active:bg-red-700 text-white font-medium px-5 py-2 rounded-lg text-sm shadow-sm shadow-red-300 transition-all flex items-center gap-2 cursor-pointer"
        >
          {isSimulating ? 'Finalizar Simulación' : 'Simulación Detenida'}
        </button>

        {/* Search button */}
        <button
          className="text-gray-400 hover:text-gray-600 p-1.5 rounded-full hover:bg-gray-100 transition-colors"
          title="Buscar"
        >
          <Search className="w-5 h-5" />
        </button>

        {/* Notification bell */}
        <button
          className="text-gray-400 hover:text-gray-600 p-1.5 rounded-full hover:bg-gray-100 transition-colors relative"
          title="Alertas"
        >
          <Bell className="w-5 h-5 text-blue-500 fill-blue-50" />
          {totalAlertas > 0 && (
            <span className="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full ring-2 ring-white"></span>
          )}
        </button>

        {/* User profile */}
        <div className="flex items-center gap-3 pl-3 border-l border-gray-200">
          <span className="text-xs font-medium text-gray-700">{userName}</span>
          <img
            src="https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&auto=format&fit=crop&q=80"
            alt={userName}
            className="w-8 h-8 rounded-full object-cover ring-1 ring-gray-300"
          />
        </div>
      </div>
    </header>
  );
};
