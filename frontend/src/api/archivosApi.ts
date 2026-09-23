import { apiClient } from './client';

/** Sube un archivo (pedidos o bloqueos) y devuelve la ruta donde el backend lo guardó, para
 * usarla luego como ParametrosSimulacion.archivoPedidos / archivoBloqueos (RF-64). */
export const archivosApi = {
  async subir(archivo: File, tipo: 'pedidos' | 'bloqueos'): Promise<string> {
    const formData = new FormData();
    formData.append('archivo', archivo);
    const response = await apiClient.post<{ rutaArchivo: string }>('/api/archivos/subir', formData, {
      params: { tipo },
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data.rutaArchivo;
  },
};
