import React, { useEffect, useState } from 'react';
import { ChevronDown, Clock, FileText } from 'lucide-react';
import { PLAZOS_POR_TIPO, TIPOS_ENTREGA } from '../../types/registro';
import type { NuevoPedidoDatos, TipoEntregaRegistro } from '../../types/registro';
import { formatFecha, formatHora, parseUbicacion } from './utils';
import { inputBase } from './estilos';
import { Campo } from './ui';

interface Props {
  onSubmit: (datos: NuevoPedidoDatos) => Promise<void>;
}

interface Errores {
  cliente?: string;
  ubicacion?: string;
  cantidad?: string;
}

export const FormularioPedido: React.FC<Props> = ({ onSubmit }) => {
  const [cliente, setCliente] = useState('');
  const [ubicacion, setUbicacion] = useState('');
  const [cantidad, setCantidad] = useState('');
  const [tipoEntrega, setTipoEntrega] = useState<TipoEntregaRegistro>(TIPOS_ENTREGA[1]);
  const [plazoHoras, setPlazoHoras] = useState<number>(PLAZOS_POR_TIPO[TIPOS_ENTREGA[1]][0]);
  const [errores, setErrores] = useState<Errores>({});
  const [ahora, setAhora] = useState(() => new Date());

  const plazosDisponibles = PLAZOS_POR_TIPO[tipoEntrega];

  const cambiarTipoEntrega = (nuevo: TipoEntregaRegistro) => {
    setTipoEntrega(nuevo);
    setPlazoHoras(PLAZOS_POR_TIPO[nuevo][0]);
  };

  // La fecha y hora de registro son automáticas: se refrescan cada 30 s
  useEffect(() => {
    const t = setInterval(() => setAhora(new Date()), 30000);
    return () => clearInterval(t);
  }, []);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    const ubic = parseUbicacion(ubicacion);
    const cant = Number(cantidad);
    const nuevos: Errores = {};
    if (!cliente.trim()) nuevos.cliente = 'Ingresa el cliente';
    if (!ubic) nuevos.ubicacion = 'Usa el formato (x,y). Ej: (45,43)';
    if (!Number.isInteger(cant) || cant < 1) nuevos.cantidad = 'Debe ser un entero mayor a 0';

    setErrores(nuevos);
    if (!ubic || Object.keys(nuevos).length > 0) return;

    await onSubmit({
      cliente: cliente.trim(),
      ubicacion: ubic,
      cantidad: cant,
      tipoEntrega,
      plazoHoras,
    });

    setCliente('');
    setUbicacion('');
    setCantidad('');
    cambiarTipoEntrega(TIPOS_ENTREGA[1]);
  };

  return (
    <form
      id="form-nuevo-pedido"
      onSubmit={handleSubmit}
      noValidate
      className="bg-white rounded-xl border border-gray-200 shadow-sm p-6"
    >
      <h2 className="flex items-center gap-2 text-lg font-bold text-gray-800 mb-4">
        <FileText className="w-5 h-5 text-blue-600" />
        Nuevo pedido
      </h2>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-x-6 gap-y-4">
        <Campo etiqueta="Cliente" error={errores.cliente}>
          <input
            type="text"
            value={cliente}
            onChange={(e) => setCliente(e.target.value)}
            placeholder="Repuestos Sur"
            className={inputBase}
          />
        </Campo>

        <Campo etiqueta="Ubicación" error={errores.ubicacion}>
          <input
            type="text"
            value={ubicacion}
            onChange={(e) => setUbicacion(e.target.value)}
            placeholder="(45,43)"
            className={inputBase}
          />
        </Campo>

        <Campo etiqueta="Fecha de registro">
          <input type="text" value={formatFecha(ahora)} readOnly className={`${inputBase} bg-gray-100`} />
        </Campo>

        <Campo etiqueta="Hora de registro">
          <div className="relative">
            <input type="text" value={formatHora(ahora)} readOnly className={`${inputBase} bg-gray-100 pr-10`} />
            <Clock className="w-5 h-5 text-gray-400 absolute right-3 top-1/2 -translate-y-1/2" />
          </div>
        </Campo>

        <Campo etiqueta="Cantidad de producto" error={errores.cantidad}>
          <input
            type="number"
            min={1}
            step={1}
            value={cantidad}
            onChange={(e) => setCantidad(e.target.value)}
            placeholder="5"
            className={inputBase}
          />
        </Campo>

        <Campo etiqueta="Tipo de entrega">
          <div className="relative">
            <select
              value={tipoEntrega}
              onChange={(e) => cambiarTipoEntrega(e.target.value as TipoEntregaRegistro)}
              className={`${inputBase} appearance-none bg-white pr-10`}
            >
              {TIPOS_ENTREGA.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
            <ChevronDown className="w-4 h-4 text-blue-500 absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none" />
          </div>
        </Campo>

        <Campo etiqueta="Plazo">
          <div className="relative">
            <select
              value={plazoHoras}
              onChange={(e) => setPlazoHoras(Number(e.target.value))}
              disabled={plazosDisponibles.length === 1}
              className={`${inputBase} appearance-none bg-white pr-10 disabled:bg-gray-100`}
            >
              {plazosDisponibles.map((h) => (
                <option key={h} value={h}>
                  {h}h
                </option>
              ))}
            </select>
            <ChevronDown className="w-4 h-4 text-blue-500 absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none" />
          </div>
        </Campo>
      </div>
    </form>
  );
};
