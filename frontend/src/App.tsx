import React, { useState } from 'react';
import { Sidebar } from './components/layout/Sidebar';
import { Header } from './components/layout/Header';
import { StockAlmacenes } from './components/almacenes/StockAlmacenes';
import { MapaOperaciones } from './components/mapa/MapaOperaciones';
import { EstadoSimulacion } from './components/simulacion/EstadoSimulacion';
import { DetalleUnidad } from './components/unidad/DetalleUnidad';
import { HistorialIncidentes } from './components/incidentes/HistorialIncidentes';

// Custom Hooks
import { useAlmacenes } from './hooks/useAlmacenes';
import { useSimulacion } from './hooks/useSimulacion';
import { useFlota } from './hooks/useFlota';
import { useIncidentes } from './hooks/useIncidentes';
import { useAlertas } from './hooks/useAlertas';
import { useMapaOperaciones } from './hooks/useMapaOperaciones';

export const App: React.FC = () => {
  const [activeSection, setActiveSection] = useState('simulacion');

  // Consumo de APIs mediante Custom Hooks
  const { almacenes, loading: loadingAlmacenes } = useAlmacenes(5000);
  const { reloj, finalizarSimulacion } = useSimulacion(3000);
  const {
    unidades,
    selectedUnidad,
    selectedUnidadId,
    setSelectedUnidadId,
    loading: loadingFlota,
  } = useFlota(4000);
  const { incidentes, loading: loadingIncidentes } = useIncidentes(5000);
  const { totalAlertas } = useAlertas(6000);
  const { pedidos } = useMapaOperaciones(4000);

  return (
    <div className="flex min-h-screen bg-[#f8fafc] text-gray-800">
      {/* 1. Sidebar lateral izquierdo */}
      <Sidebar
        activeSection={activeSection}
        onSelectSection={setActiveSection}
      />

      {/* 2. Área principal */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Cabecera Superior */}
        <Header
          onFinalizarSimulacion={finalizarSimulacion}
          totalAlertas={totalAlertas}
          isSimulating={reloj.estadoEjecucion === 'EN_EJECUCION'}
          userName="Jones Ferdinand"
        />

        {/* Contenedor central con scroll */}
        <main className="flex-1 p-6 md:p-8 max-w-7xl mx-auto w-full">
          {/* Fila superior: Stock de almacenes */}
          <StockAlmacenes
            almacenes={almacenes}
            loading={loadingAlmacenes}
          />

          {/* Grilla central: Mapa a la izquierda + Panel de Unidad a la derecha */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
            {/* Mapa de Operaciones (Ocupa 8 o 9 columnas en desktop) */}
            <div className="lg:col-span-8">
              <MapaOperaciones
                almacenes={almacenes}
                unidades={unidades}
                pedidos={pedidos}
                selectedUnidadId={selectedUnidadId}
                onSelectUnidad={setSelectedUnidadId}
              />
            </div>

            {/* Columna Derecha: Estado de Simulación + Ficha de Unidad (3 o 4 columnas) */}
            <div className="lg:col-span-4 space-y-4">
              {/* Badge de Estado de la Simulación */}
              <EstadoSimulacion reloj={reloj} />

              {/* Ficha de Detalles de la Unidad Seleccionada */}
              <DetalleUnidad unidad={selectedUnidad} />
            </div>
          </div>

          {/* Sección Inferior: Historial de Incidentes */}
          <HistorialIncidentes
            incidentes={incidentes}
            loading={loadingIncidentes}
          />
        </main>
      </div>
    </div>
  );
};

export default App;
