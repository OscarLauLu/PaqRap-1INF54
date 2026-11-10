import type { MetricasSimulacion } from '../types';
import type { RutaApi } from '../api/rutasApi';
import { parsearFechaLocal } from './fechaLocal';

/**
 * Reporte diario automático de la Operación día a día (G09).
 * Se genera solo cuando cambia el día de la operación y al detenerla (■).
 * Se guarda en este navegador (localStorage) y se muestra en Reportes.
 */

export interface EntregaDia {
  pedido: string;
  unidad: string;
  salida: string;   // hora en que el vehículo salió del almacén
  llegada: string;  // hora en que llegó al cliente
  minutos: number;  // tiempo que le tomó entregar ese pedido (salida → llegada)
}

interface Acumulado {
  enPlazo: number;
  tarde: number;
  costo: number;
  costoAuto: number;
  costoMoto: number;
  costoBici: number;
  averias: number;
  bloqueos: number;
}

export interface ReporteDiario {
  id: string;
  fecha: string;                         // día de la operación (AAAA-MM-DD)
  generado: string;                      // momento real en que se generó
  motivo: 'CAMBIO_DE_DIA' | 'CIERRE';
  entregados: number;
  enPlazo: number;
  tarde: number;
  pctATiempo: number | null;
  costo: number;
  costoAuto: number;
  costoMoto: number;
  costoBici: number;
  averias: number;
  bloqueos: number;
  tiempoPromedioMin: number | null;
  entregas: EntregaDia[];
  acumulado: Acumulado;                  // totales del motor en ese momento (para calcular el siguiente día)
}

const CLAVE = 'paqrap.reportesDiarios';
export const EVENTO_REPORTES = 'paqrap:reportes-diarios';

export const listarReportesDiarios = (): ReporteDiario[] => {
  try {
    const v = JSON.parse(localStorage.getItem(CLAVE) || '[]');
    return Array.isArray(v) ? v : [];
  } catch {
    return [];
  }
};

const guardar = (lista: ReporteDiario[]) => {
  try {
    localStorage.setItem(CLAVE, JSON.stringify(lista.slice(-60)));
  } catch {
    // sin espacio o sin permiso: se ignora
  }
  window.dispatchEvent(new Event(EVENTO_REPORTES));
};

/** Formato corto de minutos: "45 min" o "1 h 25 min" */
export const formatMinutos = (min: number | null | undefined): string => {
  if (min === null || min === undefined || !Number.isFinite(min)) return '—';
  const m = Math.max(0, Math.round(min));
  if (m < 60) return `${m} min`;
  const h = Math.floor(m / 60);
  const r = m % 60;
  return r > 0 ? `${h} h ${r} min` : `${h} h`;
};

/** Entregas hechas en un día: tiempo de cada pedido desde que el vehículo salió del almacén hasta que llegó al cliente */
export const entregasDelDia = (rutas: RutaApi[], fecha: string): EntregaDia[] => {
  const lista: EntregaDia[] = [];
  rutas.forEach((r) => {
    const salidaMs = parsearFechaLocal(r.fechaHoraGeneracion);
    (r.paradas || []).forEach((p) => {
      if (!p.entregada || !p.horaEstimadaLlegada || p.horaEstimadaLlegada.slice(0, 10) !== fecha) return;
      const llegadaMs = parsearFechaLocal(p.horaEstimadaLlegada);
      if (salidaMs === null || llegadaMs === null || llegadaMs < salidaMs) return;
      lista.push({
        pedido: p.codigoPedido,
        unidad: r.unidadCodigo,
        salida: r.fechaHoraGeneracion ?? '',
        llegada: p.horaEstimadaLlegada,
        minutos: Math.round((llegadaMs - salidaMs) / 60000),
      });
    });
  });
  return lista.sort((a, b) => a.llegada.localeCompare(b.llegada));
};

/** Genera (o actualiza) el reporte de un día y lo guarda */
export const generarReporteDiario = (args: {
  fecha: string;
  motivo: 'CAMBIO_DE_DIA' | 'CIERRE';
  metricas: MetricasSimulacion;
  rutas: RutaApi[];
}): ReporteDiario => {
  const { fecha, motivo, metricas, rutas } = args;
  const lista = listarReportesDiarios();
  // El día anterior guardado: sus totales sirven para saber cuánto se hizo solo en este día
  const previo = [...lista].filter((r) => r.fecha < fecha).sort((a, b) => a.fecha.localeCompare(b.fecha)).pop();

  const actual: Acumulado = {
    enPlazo: metricas.pedidosEnPlazo ?? 0,
    tarde: metricas.pedidosTarde ?? 0,
    costo: metricas.costoAcumuladoTotal ?? 0,
    costoAuto: metricas.costoAuto ?? 0,
    costoMoto: metricas.costoMoto ?? 0,
    costoBici: metricas.costoBici ?? 0,
    averias: metricas.averiasOcurridas ?? 0,
    bloqueos: metricas.bloqueosOcurridos ?? 0,
  };
  // Si el motor se reinició (los totales bajaron), se usan los totales tal cual
  const base = previo && previo.acumulado && actual.costo >= previo.acumulado.costo ? previo.acumulado : null;
  const delta = (k: keyof Acumulado) => Math.max(0, actual[k] - (base ? base[k] : 0));

  const entregas = entregasDelDia(rutas, fecha);
  const enPlazo = delta('enPlazo');
  const tarde = delta('tarde');
  const reporte: ReporteDiario = {
    id: `${fecha}-${Date.now()}`,
    fecha,
    generado: new Date().toISOString(),
    motivo,
    entregados: enPlazo + tarde,
    enPlazo,
    tarde,
    pctATiempo: enPlazo + tarde > 0 ? (enPlazo / (enPlazo + tarde)) * 100 : null,
    costo: delta('costo'),
    costoAuto: delta('costoAuto'),
    costoMoto: delta('costoMoto'),
    costoBici: delta('costoBici'),
    averias: delta('averias'),
    bloqueos: delta('bloqueos'),
    tiempoPromedioMin: entregas.length > 0 ? entregas.reduce((s, e) => s + e.minutos, 0) / entregas.length : null,
    entregas,
    acumulado: actual,
  };

  // Un solo reporte por día: el más reciente reemplaza al anterior del mismo día
  guardar([...lista.filter((r) => r.fecha !== fecha), reporte].sort((a, b) => a.fecha.localeCompare(b.fecha)));
  return reporte;
};

/** Descarga un reporte diario en CSV (resumen + tiempo de cada entrega) */
export const descargarReporteDiarioCSV = (r: ReporteDiario) => {
  const filas: (string | number)[][] = [
    ['Reporte diario de la operación', r.fecha],
    ['Generado', new Date(r.generado).toLocaleString('es-PE'), r.motivo === 'CIERRE' ? 'Al detener la operación' : 'Automático al cambiar de día'],
    [],
    ['Pedidos entregados', r.entregados],
    ['Entregados a tiempo', r.enPlazo],
    ['Entregados tarde', r.tarde],
    ['% a tiempo', r.pctATiempo === null ? '' : r.pctATiempo.toFixed(1)],
    ['Tiempo promedio de entrega (min)', r.tiempoPromedioMin === null ? '' : r.tiempoPromedioMin.toFixed(0)],
    ['Costo del día (S/)', r.costo.toFixed(2)],
    ['Costo autos (S/)', r.costoAuto.toFixed(2)],
    ['Costo motos (S/)', r.costoMoto.toFixed(2)],
    ['Costo bicicletas (S/)', r.costoBici.toFixed(2)],
    ['Averías', r.averias],
    ['Bloqueos', r.bloqueos],
    [],
    ['Pedido', 'Vehículo', 'Salida del almacén', 'Llegada al cliente', 'Tiempo de entrega (min)'],
    ...r.entregas.map((e) => [e.pedido, e.unidad, e.salida, e.llegada, e.minutos]),
  ];
  const csv = filas.map((f) => f.map((c) => `"${String(c).replace(/"/g, '""')}"`).join(',')).join('\n');
  const url = URL.createObjectURL(new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8' }));
  const a = document.createElement('a');
  a.href = url;
  a.download = `reporte-diario-${r.fecha}.csv`;
  a.click();
  URL.revokeObjectURL(url);
};