import { useCallback, useState } from 'react';

/** Como useState, pero recuerda el valor en el navegador (localStorage). */
export function usePersistente<T>(clave: string, inicial: T, esValido?: (valor: unknown) => boolean) {
  const [valor, setValor] = useState<T>(() => {
    try {
      const guardado = localStorage.getItem(clave);
      if (guardado !== null) {
        const leido: unknown = JSON.parse(guardado);
        if (!esValido || esValido(leido)) return leido as T;
      }
    } catch {
      // sin acceso al almacenamiento: se usa el valor inicial
    }
    return inicial;
  });

  const guardar = useCallback(
    (nuevo: T) => {
      setValor(nuevo);
      try {
        localStorage.setItem(clave, JSON.stringify(nuevo));
      } catch {
        // ignorar
      }
    },
    [clave],
  );

  return [valor, guardar] as const;
}
