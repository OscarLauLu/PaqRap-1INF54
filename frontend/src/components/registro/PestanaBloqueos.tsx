import React, { useRef, useState } from 'react';
import { FileText, FileUp, Info, Upload } from 'lucide-react';
import { bloqueosApi } from '../../api/bloqueosApi';
import { Aviso } from './ui';

export const PestanaBloqueos: React.FC = () => {
  const [archivo, setArchivo] = useState<File | null>(null);
  const [mensaje, setMensaje] = useState<string | null>(null);
  const [procesando, setProcesando] = useState(false);
  const inputArchivo = useRef<HTMLInputElement>(null);

  const abrirSelector = () => inputArchivo.current?.click();

  const handleArchivo = (e: React.ChangeEvent<HTMLInputElement>) => {
    const elegido = e.target.files?.[0];
    if (elegido) {
      setArchivo(elegido);
      setMensaje(null);
    }
    e.target.value = '';
  };

  const registrar = async () => {
    if (!archivo) return;
    setProcesando(true);
    try {
      const { cantidad } = await bloqueosApi.registrarArchivo(archivo);
      setMensaje(
        `Archivo "${archivo.name}" leído: ${cantidad} línea(s). Modo demostración: aún no se guarda en el servidor.`,
      );
      setArchivo(null);
    } finally {
      setProcesando(false);
    }
  };

  return (
    <>
      <input ref={inputArchivo} type="file" accept=".txt,.csv" onChange={handleArchivo} className="hidden" />

      <section className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
        <h2 className="flex items-center gap-2 text-lg font-bold text-gray-800 mb-4">
          <FileText className="w-5 h-5 text-blue-600" />
          Nuevo bloqueo
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 items-center">
          <div className="flex justify-center">
            <button
              type="button"
              onClick={abrirSelector}
              className="w-48 h-36 rounded-xl border-2 border-blue-500 bg-white hover:bg-blue-50 transition-colors flex flex-col items-center justify-between py-4 px-2"
            >
              <span className="font-bold text-blue-600">Bloqueos</span>
              <FileUp className="w-9 h-9 text-blue-600" />
              <span className="text-center leading-tight">
                {archivo ? (
                  <>
                    <span className="block font-bold text-green-500">cargado</span>
                    <span className="block text-xs text-gray-500 max-w-40 truncate">{archivo.name}</span>
                  </>
                ) : (
                  <span className="font-semibold text-gray-400">sin archivo</span>
                )}
              </span>
            </button>
          </div>

          <div className="flex items-center gap-3 rounded-lg bg-blue-50 px-6 py-8 text-sm font-semibold text-gray-600">
            <Info className="w-6 h-6 text-blue-600 shrink-0" />
            <p>El archivo a subir debe contener los bloqueos de las calles en el formato adecuado.</p>
          </div>
        </div>
      </section>

      <div className="flex justify-end items-center gap-3">
        <button
          type="button"
          onClick={abrirSelector}
          className="flex items-center gap-2 rounded-xl bg-gray-500 hover:bg-gray-600 text-white font-semibold px-5 py-2.5 text-sm transition-colors"
        >
          <Upload className="w-4 h-4" />
          Cargar archivo
        </button>
        <button
          type="button"
          onClick={registrar}
          disabled={!archivo || procesando}
          className="rounded-xl bg-blue-600 hover:bg-blue-700 active:bg-blue-800 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold px-6 py-2.5 text-sm transition-colors"
        >
          Registrar bloqueo
        </button>
      </div>

      {mensaje && <Aviso mensaje={mensaje} onCerrar={() => setMensaje(null)} />}
    </>
  );
};
