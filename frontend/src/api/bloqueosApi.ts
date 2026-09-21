/**
 * PASO 1: solo lee el archivo en el navegador y cuenta las líneas.
 * PASO 4: reemplazar por un POST multipart al backend (ej. /api/bloqueos/cargar)
 * usando apiClient, sin tocar el componente.
 */
export const bloqueosApi = {
  async registrarArchivo(archivo: File): Promise<{ cantidad: number }> {
    const texto = await archivo.text();
    const cantidad = texto.split(/\r?\n/).filter((linea) => linea.trim() !== '').length;
    return { cantidad };
  },
};
