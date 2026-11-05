/**
 * Convierte un LocalDateTime del backend ("2026-10-15T09:26:15", con o sin fracción de segundo,
 * o el arreglo [2026, 10, 15, 9, 26, 15]) a milisegundos en la hora local del navegador.
 * Todos los instantes del simulador usan la misma convención, así que se pueden comparar entre sí.
 * Devuelve null si el valor no es una fecha válida.
 */
export function parsearFechaLocal(valor: unknown): number | null {
  if (Array.isArray(valor) && valor.length >= 3) {
    const [a, m, d, h = 0, mi = 0, s = 0] = valor.map(Number);
    const t = new Date(a, m - 1, d, h, mi, s).getTime();
    return Number.isFinite(t) ? t : null;
  }
  if (typeof valor !== 'string') return null;
  const r = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2})(?:\.(\d{1,3})\d*)?)?/.exec(valor);
  if (!r) return null;
  const ms = r[7] ? Number(r[7].padEnd(3, '0')) : 0;
  const t = new Date(+r[1], +r[2] - 1, +r[3], +r[4], +r[5], r[6] ? +r[6] : 0, ms).getTime();
  return Number.isFinite(t) ? t : null;
}
