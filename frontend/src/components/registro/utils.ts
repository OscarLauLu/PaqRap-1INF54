import type { Ubicacion } from '../../types';

const pad = (n: number) => String(n).padStart(2, '0');

export const formatFecha = (d: Date) =>
  `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`;

export const formatHora = (d: Date) => `${pad(d.getHours())}:${pad(d.getMinutes())}`;

export const formatFechaHora = (iso: string) => {
  const d = new Date(iso);
  return `${formatFecha(d)} ${formatHora(d)}`;
};

/** Fecha de vencimiento = fecha de registro + plazo (en horas). */
export const calcularVencimiento = (iso: string, plazoHoras: number) =>
  new Date(new Date(iso).getTime() + plazoHoras * 3600000);

export const formatVencimiento = (iso: string, plazoHoras: number) => {
  const d = calcularVencimiento(iso, plazoHoras);
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)} ${formatHora(d)}`;
};

export const formatUbicacion = (u: Ubicacion) => `(${u.posX},${u.posY})`;

/** Acepta "(45,43)", "45,43" o "45, 43". Devuelve null si el formato no es válido. */
export const parseUbicacion = (texto: string): Ubicacion | null => {
  const m = texto.trim().match(/^\(?\s*(\d+)\s*,\s*(\d+)\s*\)?$/);
  return m ? { posX: Number(m[1]), posY: Number(m[2]) } : null;
};

/** Fecha de hoy en formato yyyy-mm-dd (el que usa <input type="date">). */
export const hoyISO = () => {
  const d = new Date();
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
};

export const ahoraHHmm = () => formatHora(new Date());

/** Suma horas a un "HH:mm" (da la vuelta a las 24 h). */
export const sumarHoras = (hhmm: string, horas: number) => {
  const [h = 0, m = 0] = hhmm.split(':').map(Number);
  return `${pad((h + horas) % 24)}:${pad(m)}`;
};

/** Une "yyyy-mm-dd" + "HH:mm" en una fecha local. */
export const combinarFechaHora = (fecha: string, hora: string) => new Date(`${fecha}T${hora}`);
