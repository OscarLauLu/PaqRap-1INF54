// ===== Movimiento continuo según el horario de la ruta =====
// Con el horario (salida, llegadas y acondicionamientos) y el reloj simulado, la posición se calcula
// en cada cuadro: el vehículo avanza parejo aunque el backend solo informe cada pocos segundos.
// Se siguen las mismas reglas que el motor: entre paradas avanza a ritmo constante, en cada parada se
// queda la hora de acondicionamiento y durante la alimentación (12-13, 19-20 y 03-04) se detiene.

/** Distancia sobre el camino (s, km) en cada instante clave (t, ms), en orden de tiempo */
export type Horario = { s: number[]; t: number[] };

/** Horario de una ruta: salida del almacén y, por parada, índice en el camino, llegada y fin del acondicionamiento */
export type HorarioRuta = { salida: number; paradas: Array<{ idx: number; llegada: number; salida: number }> };

/**
 * Arma el horario sobre un trazado cuyo arreglo "acum" tiene los km acumulados en cada esquina del camino.
 * Devuelve null si alguna parada no cae dentro del camino.
 */
export const crearHorario = (tr: { acum: number[] }, h: HorarioRuta): Horario | null => {
  const s = [0];
  const t = [h.salida];
  for (const p of h.paradas) {
    if (p.idx < 0 || p.idx >= tr.acum.length) return null;
    const sp = tr.acum[p.idx];
    const llegada = Math.max(p.llegada, t[t.length - 1]); // el tiempo nunca retrocede
    s.push(sp, sp);
    t.push(llegada, Math.max(p.salida, llegada));
  }
  return { s, t };
};

/** Distancia recorrida en el instante simulado "ms" según el horario */
export const distanciaEn = (h: Horario, ms: number): number => {
  if (ms <= h.t[0]) return h.s[0];
  for (let k = 1; k < h.t.length; k++) {
    if (ms < h.t[k]) {
      const dt = h.t[k] - h.t[k - 1];
      const f = dt > 0 ? (ms - h.t[k - 1]) / dt : 1;
      return h.s[k - 1] + (h.s[k] - h.s[k - 1]) * f;
    }
  }
  return h.s[h.s.length - 1];
};

/** Igual que el motor (Turno.esHoraAlimentacion): en esas horas los vehículos se detienen donde estaban */
const HORAS_ALIMENTACION = new Set([3, 12, 19]);
export const sinAlimentacion = (ms: number): number => {
  const d = new Date(ms);
  if (!HORAS_ALIMENTACION.has(d.getHours())) return ms;
  d.setMinutes(0, 0, 0);
  return d.getTime();
};
