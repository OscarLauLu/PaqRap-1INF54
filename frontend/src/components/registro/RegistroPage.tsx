import React, { useRef, useState } from 'react';
import { Car, ClipboardList, Construction, Upload, Wrench } from 'lucide-react';
import { usePedidosRegistro } from '../../hooks/usePedidosRegistro';
import type { NuevoPedidoDatos } from '../../types/registro';
import { FormularioPedido } from './FormularioPedido';
import { TablaPedidos } from './TablaPedidos';
import { PestanaAverias } from './PestanaAverias';
import { PestanaBloqueos } from './PestanaBloqueos';
import { Aviso } from './ui';
import { calcularVencimiento, formatFechaHora } from './utils';

type Pestana = 'pedidos' | 'bloqueos' | 'averias';

const PESTANAS: { id: Pestana; titulo: string; detalle: string; Icono: React.ElementType }[] = [
  { id: 'pedidos', titulo: 'Pedidos de P', detalle: 'Entrega de producto', Icono: Car },
  { id: 'bloqueos', titulo: 'Bloqueos', detalle: 'Calle cerrada', Icono: Construction },
  { id: 'averias', titulo: 'Averías', detalle: 'Vehículos dañados', Icono: Wrench },
];

export const RegistroPage: React.FC = () => {
  const [pestana, setPestana] = useState<Pestana>('pedidos');
  const [mensaje, setMensaje] = useState<string | null>(null);
  const { pedidos, cargando, registrar } = usePedidosRegistro();
  const inputArchivo = useRef<HTMLInputElement>(null);

  const handleRegistrar = async (datos: NuevoPedidoDatos) => {
    const nuevo = await registrar(datos);
    const vence = calcularVencimiento(nuevo.fechaRegistro, nuevo.plazoHoras).toISOString();
    setMensaje(`Pedido registrado para ${nuevo.cliente}. Vence el ${formatFechaHora(vence)}.`);
  };

  const handleArchivo = (e: React.ChangeEvent<HTMLInputElement>) => {
    const archivo = e.target.files?.[0];
    if (archivo) {
      // TODO paso 5: leer el archivo y registrar los pedidos
      setMensaje(`Archivo "${archivo.name}" seleccionado. La importación se conecta en el paso 5.`);
    }
    e.target.value = '';
  };

  return (
    <main className="flex-1 p-6 md:p-8 space-y-6">
      {/* Pestañas */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {PESTANAS.map(({ id, titulo, detalle, Icono }) => {
          const activa = pestana === id;
          return (
            <button
              key={id}
              type="button"
              onClick={() => {
                setPestana(id);
                setMensaje(null);
              }}
              className={`flex items-center gap-4 rounded-xl border px-5 py-4 text-left transition-colors ${
                activa
                  ? 'border-blue-500 bg-blue-50 shadow-sm'
                  : 'border-gray-200 bg-white hover:bg-gray-50'
              }`}
            >
              <Icono className={`w-10 h-10 shrink-0 ${activa ? 'text-blue-600' : 'text-gray-800'}`} />
              <span>
                <span className={`block text-lg font-bold ${activa ? 'text-blue-600' : 'text-gray-800'}`}>
                  {titulo}
                </span>
                <span className="block text-sm text-gray-500">{detalle}</span>
              </span>
            </button>
          );
        })}
      </div>

      {pestana === 'pedidos' && (
        <>
          <FormularioPedido onSubmit={handleRegistrar} />

          <div className="flex flex-wrap items-center justify-between gap-3">
            <h2 className="flex items-center gap-2 text-lg font-bold text-gray-800">
              <ClipboardList className="w-5 h-5 text-blue-600" />
              Lista de pedidos
            </h2>
            <div className="flex items-center gap-3">
              <input
                ref={inputArchivo}
                type="file"
                accept=".txt,.csv"
                onChange={handleArchivo}
                className="hidden"
              />
              <button
                type="button"
                onClick={() => inputArchivo.current?.click()}
                className="flex items-center gap-2 rounded-xl bg-gray-500 hover:bg-gray-600 text-white font-semibold px-5 py-2.5 text-sm transition-colors"
              >
                <Upload className="w-4 h-4" />
                Cargar archivo
              </button>
              <button
                type="submit"
                form="form-nuevo-pedido"
                className="rounded-xl bg-blue-600 hover:bg-blue-700 active:bg-blue-800 text-white font-semibold px-6 py-2.5 text-sm transition-colors"
              >
                Registrar pedido
              </button>
            </div>
          </div>

          {mensaje && <Aviso mensaje={mensaje} onCerrar={() => setMensaje(null)} />}

          <TablaPedidos pedidos={pedidos} cargando={cargando} />
        </>
      )}

      {pestana === 'bloqueos' && <PestanaBloqueos />}
      {pestana === 'averias' && <PestanaAverias />}
    </main>
  );
};
