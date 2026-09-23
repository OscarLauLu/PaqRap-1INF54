import React, { useRef, useState } from 'react';
import { Car, ClipboardList, Construction, Loader2, Upload, Wrench } from 'lucide-react';
import { usePedidosRegistro } from '../../hooks/usePedidosRegistro';
import type { NuevoPedidoDatos } from '../../types/registro';
import { archivosApi } from '../../api/archivosApi';
import { FormularioPedido } from './FormularioPedido';
import { TablaPedidos } from './TablaPedidos';
import { PestanaAverias } from './PestanaAverias';
import { PestanaBloqueos } from './PestanaBloqueos';
import { StatTile } from '../ui/Card';
import { Aviso } from './ui';
import { formatFechaHora } from './utils';

type Pestana = 'pedidos' | 'bloqueos' | 'averias';

const PESTANAS: { id: Pestana; titulo: string; detalle: string; Icono: React.ElementType }[] = [
  { id: 'pedidos', titulo: 'Pedidos de P', detalle: 'Entrega de producto', Icono: Car },
  { id: 'bloqueos', titulo: 'Bloqueos', detalle: 'Calle cerrada', Icono: Construction },
  { id: 'averias', titulo: 'Averías', detalle: 'Vehículos dañados', Icono: Wrench },
];

export const RegistroPage: React.FC = () => {
  const [pestana, setPestana] = useState<Pestana>('pedidos');
  const [mensaje, setMensaje] = useState<string | null>(null);
  const [subiendoArchivo, setSubiendoArchivo] = useState(false);
  const { pedidos, cargando, registrar } = usePedidosRegistro();
  const inputArchivo = useRef<HTMLInputElement>(null);

  const handleRegistrar = async (datos: NuevoPedidoDatos) => {
    const nuevo = await registrar(datos);
    setMensaje(
      nuevo.plazoLimiteEntrega
        ? `Pedido ${nuevo.codigo} registrado para ${nuevo.cliente}. Vence el ${formatFechaHora(nuevo.plazoLimiteEntrega)}.`
        : `Pedido ${nuevo.codigo} registrado para ${nuevo.cliente}.`,
    );
  };

  const handleArchivo = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const archivo = e.target.files?.[0];
    e.target.value = '';
    if (!archivo) return;
    setSubiendoArchivo(true);
    try {
      await archivosApi.subir(archivo, 'pedidos');
      setMensaje(`Archivo "${archivo.name}" subido. Quedará disponible para usarse al configurar la simulación.`);
    } catch {
      setMensaje('No se pudo subir el archivo.');
    } finally {
      setSubiendoArchivo(false);
    }
  };

  const totalPedidos = pedidos.length;
  const porAtender = pedidos.filter((p) => p.estado === 'REGISTRADO').length;
  const planificados = pedidos.filter((p) => p.estado === 'PLANIFICADO' || p.estado === 'EN_RUTA').length;
  const entregados = pedidos.filter((p) => p.estado === 'ENTREGADO').length;

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
                  ? 'border-(--color-brand-500) bg-(--color-brand-50) shadow-sm'
                  : 'border-(--color-line-200) bg-white hover:bg-gray-50'
              }`}
            >
              <Icono className={`w-10 h-10 shrink-0 ${activa ? 'text-(--color-brand-500)' : 'text-(--color-ink-900)'}`} />
              <span>
                <span className={`block text-lg font-bold ${activa ? 'text-(--color-brand-500)' : 'text-(--color-ink-900)'}`}>
                  {titulo}
                </span>
                <span className="block text-sm text-(--color-ink-400)">{detalle}</span>
              </span>
            </button>
          );
        })}
      </div>

      {pestana === 'pedidos' && (
        <>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <StatTile etiqueta="Total pedidos" valor={totalPedidos} tono="azul" />
            <StatTile etiqueta="Por atender" valor={porAtender} tono="ambar" />
            <StatTile etiqueta="Planificados / en ruta" valor={planificados} tono="neutro" />
            <StatTile etiqueta="Entregados" valor={entregados} tono="verde" />
          </div>

          <FormularioPedido onSubmit={handleRegistrar} />

          <div className="flex flex-wrap items-center justify-between gap-3">
            <h2 className="flex items-center gap-2 text-lg font-bold text-(--color-ink-900)">
              <ClipboardList className="w-5 h-5 text-(--color-brand-500)" />
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
                disabled={subiendoArchivo}
                className="flex items-center gap-2 rounded-xl bg-gray-500 hover:bg-gray-600 disabled:opacity-50 text-white font-semibold px-5 py-2.5 text-sm transition-colors"
              >
                {subiendoArchivo ? <Loader2 className="w-4 h-4 animate-spin" /> : <Upload className="w-4 h-4" />}
                Cargar archivo
              </button>
              <button
                type="submit"
                form="form-nuevo-pedido"
                className="rounded-xl bg-(--color-brand-500) hover:bg-(--color-brand-600) active:bg-(--color-brand-700) text-white font-semibold px-6 py-2.5 text-sm transition-colors"
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
