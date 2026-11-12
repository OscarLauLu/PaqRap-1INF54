import React, { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { Almacen, BloqueoVial, Pedido, Ubicacion, UnidadTransporte } from '../../types';
import { Info, Maximize2, Minus, Plus, X } from 'lucide-react';
import { LeyendaMapa } from './LeyendaMapa';
import { Bodega, PinPedido, VehiculoRealista } from './IconosMapa';
import { pasaFiltroAlmacen, pasaFiltroUnidad, nivelSemaforoAlmacen, nivelSemaforoUT, type FiltrosPanel } from '../../hooks/filtros';
import { parsearFechaLocal } from '../../utils/fechaLocal';
import { averiasApi } from '../../api/averiasApi';
import { crearHorario, distanciaEn, sinAlimentacion, suavizarReloj, type Horario, type HorarioRuta } from './movimiento';

/** Recorrido planificado de un vehículo: sus paradas en orden (empezando por su origen). */
export interface RutaMapa {
  unidadCodigo: string;
  puntos: Ubicacion[];
  /** Códigos de los pedidos que aún faltan entregar en esta ruta */
  codigosPedidos?: string[];
  /** Indica que "puntos" ya es el camino calle por calle, así que se dibuja sin calcular nada */
  esCamino?: boolean;
  /** Camino completo de la ruta (desde el almacén): el vehículo se anima sobre él, siguiendo las calles */
  caminoCompleto?: Ubicacion[];
  /**
   * Horario de la ruta (ms en hora local del simulador): salida del almacén y, por parada, su índice
   * en caminoCompleto, la llegada y el fin del acondicionamiento. Con esto el mapa calcula la posición
   * del vehículo en cada cuadro, sin esperar a la siguiente consulta al backend.
   */
  horario?: HorarioRuta;
  /** Ruta de rescate: código de la unidad averiada cuya carga va a trasvasar */
  rescataA?: string;
}

/** Reloj simulado que recibe el mapa: con él se estima la hora simulada entre consultas */
export interface RelojMapa {
  instanteActual?: string;
  factorAceleracion?: number;
  /** false en pausa o sin simulación: los vehículos se quedan quietos */
  corriendo: boolean;
}

interface MapaOperacionesProps {
  almacenes: Almacen[];
  unidades: UnidadTransporte[];
  pedidos: Pedido[];
  bloqueos?: BloqueoVial[];
  rutas?: RutaMapa[];
  selectedUnidadCodigo: string | null;
  onSelectUnidad: (codigo: string) => void;
  selectedAlmacenCodigo?: string | null;
  onSelectAlmacen?: (codigo: string) => void;
  selectedPedidoCodigo?: string | null;
  onSelectPedido?: (codigo: string) => void;
  /** Nombre del escenario para el título de la leyenda (opcional) */
  escenario?: string;
  /** Si la simulación está en pausa, los vehículos se congelan donde están */
  pausado?: boolean;
  /** Reloj simulado: si llega, los vehículos con horario se mueven de forma continua */
  reloj?: RelojMapa;
  /** Filtros del panel derecho: lo filtrado allí también se oculta en el mapa */
  filtros?: FiltrosPanel;
  /** En la operación diaria muestra pedidos por atender */
  mostrarPorAtender?: boolean;
  /** Averías registradas (opcional): sirve para saber el tipo de avería de cada vehículo */
  averias?: Array<{ idUnidad?: string; unidadCodigo?: string; tipo?: string; tipoAveria?: string; activa?: boolean }>;
  /** Indicadores globales de ocupación (flota y almacenes), con su color de semáforo */
  ocupacionGlobal?: {
    flota: { pct: number; color: string; detalle?: string };
    almacenes: { pct: number; color: string; detalle?: string };
  };
}

/** Extrae los pares "x,y" de un texto como "(30,15) → (35,15)" o "30,15,35,15". */
const parsePuntos = (texto: string): Array<[number, number]> =>
  Array.from(
    (texto || '').matchAll(/(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)/g),
    (m) => [Number(m[1]), Number(m[2])] as [number, number],
  );

// Área total: la ciudad mide 70 × 50 km, con 1 km de margen alrededor
const BASE = { x: -1, y: -1, w: 72, h: 52 };
// Tamaño de los cuadros: 2 = cada cuadro de 1 km se ve el doble de grande que con la ciudad
// completa en pantalla. Súbelo o bájalo (p. ej. 1.5 o 2.5) para ajustar el tamaño.
const ESCALA = 1.2; // zoom con el que arranca el mapa
// Zoom: el usuario puede acercar o alejar de forma continua (rueda del mouse o botones + / −)
const ZOOM_MIN = 1;
const ZOOM_MAX = 8;
const anchoVista = (zoom: number) => BASE.w / zoom;
const altoVista = (zoom: number) => BASE.h / zoom;


// ===== Orientación de los vehículos (estilo GPS) =====
// ===== Paleta del mapa: colores vibrantes y distintos para cada elemento =====
// Pedidos (semáforo): verde, ámbar y rojo · Vehículos: azul, morado y rosa
// Almacenes: naranja, cian e índigo · Avería: amarillo con negro (como señal de peligro)
const COLOR_TIPO: Record<string, string> = { Auto: '#2563EB', Moto: '#9333EA', Bicicleta: '#EC4899' };
const COLOR_ALMACEN = { central: '#EA580C', norte: '#0891B2', este: '#4F46E5' };
// Avería por tipo (enunciado). Escala de riesgo: amarillo (bajo) → naranja (medio) → rojo oscuro (alto),
// más el número del tipo, para distinguirlas sin depender solo del color
const AVERIA_TIPO: Record<string, { color: string; texto: string; nombre: string; detalle: string; riesgo: string }> = {
  '1': { color: '#FDE047', texto: '#713F12', nombre: 'Tipo 1 · leve', detalle: '2 h en el lugar y sigue su ruta', riesgo: 'Riesgo bajo' },
  '2': { color: '#F97316', texto: '#FFFFFF', nombre: 'Tipo 2 · moderada', detalle: '4 h en el lugar; vuelve al final del turno siguiente', riesgo: 'Riesgo medio' },
  '3': { color: '#B91C1C', texto: '#FFFFFF', nombre: 'Tipo 3 · grave', detalle: '4 h en el lugar; 2 días o más en taller', riesgo: 'Riesgo alto' },
};
/** Normaliza "TIPO_2", "2", "tipo 2"… a "1" | "2" | "3" (o null si no se sabe) */
const numeroTipoAveria = (valor: unknown): string | null => {
  const m = /([123])/.exec(String(valor ?? ''));
  return m ? m[1] : null;
};
// Color del pin de cada pedido según su criticidad (mismos colores del semáforo)
const COLOR_CRITICIDAD: Record<string, string> = { VERDE: '#22B957', AMBAR: '#F5B900', ROJO: '#E53935' };
// Todos los íconos (pedidos, vehículos y almacenes) miden lo mismo en el mapa: un tamaño mediano,
// para que ninguno tape a los demás ni llame más la atención que el resto.
const TAMANO_ICONO = 1.5; // km
const ESCALA_VEHICULO = TAMANO_ICONO / 36; // el auto mide 36 unidades de largo en su dibujo
const ESCALA_PIN = TAMANO_ICONO / 32;      // el pin mide 32 de alto
// Los almacenes van un poco más grandes (+30 %) porque son puntos de referencia fijos del mapa
const ESCALA_BODEGA = (TAMANO_ICONO * 1.3) / 34; // la bodega mide 34 de lado
// Debe coincidir con cada cuánto se consulta la flota (useFlota(4000) en App.tsx):
// así el vehículo se desliza justo hasta que llega la siguiente posición.
const DURACION_MOVIMIENTO_MS = 4000;
// true: solo se dibujan los vehículos con trabajo (con ruta, en ruta, en movimiento o averiados).
// false: se dibujan TODOS. Sirve para revisar si un vehículo "no aparece" por este filtro.
const SOLO_VEHICULOS_CON_TRABAJO = true;

/** Ángulo de marcha en grados (0° = este, 90° = norte) a partir de un desplazamiento */
const anguloDe = (dx: number, dy: number): number | null =>
  dx === 0 && dy === 0 ? null : (Math.atan2(dy, dx) * 180) / Math.PI;

/** Ángulo hacia el siguiente punto de su ruta: sirve cuando el vehículo aún no se ha movido */
const anguloPorRuta = (u: UnidadTransporte, r?: RutaMapa): number | null => {
  if (!r) return null;
  for (let i = 1; i < r.puntos.length; i++) {
    const desde = r.esCamino ? r.puntos[i - 1] : u.ubicacionActual;
    const ang = anguloDe(r.puntos[i].posX - desde.posX, r.puntos[i].posY - desde.posY);
    if (ang !== null) return ang;
  }
  return null;
};

/** Elige el giro más corto (por ejemplo, de 350° a 10° gira 20° y no 340°) */
const giroMasCorto = (anterior: number, nuevo: number) => anterior + ((((nuevo - anterior) % 360) + 540) % 360) - 180;

// ===== Movimiento sobre la ruta =====
// El backend solo envía la posición cada pocos segundos (en la 5D, unos 10 minutos simulados).
// Si el vehículo se deslizara en línea recta entre dos posiciones, cruzaría las manzanas "flotando".
// Por eso, cuando tiene ruta, se proyecta su posición sobre el camino y se anima recorriendo
// ese camino calle por calle, hasta llegar a la nueva posición.
type Trazado = { pts: Array<[number, number]>; acum: number[]; total: number; clave: string };

/** Prepara un camino para recorrerlo por distancia (km acumulados en cada esquina) */
const crearTrazado = (puntos: Ubicacion[], clave: string): Trazado | null => {
  const pts = puntos.map((p) => [p.posX, p.posY] as [number, number]);
  if (pts.length < 2) return null;
  const acum = [0];
  for (let i = 1; i < pts.length; i++) {
    acum.push(acum[i - 1] + Math.hypot(pts[i][0] - pts[i - 1][0], pts[i][1] - pts[i - 1][1]));
  }
  const total = acum[acum.length - 1];
  return total > 0 ? { pts, acum, total, clave } : null;
};

/** Distancia sobre el camino del punto más cercano a (x, y), sin retroceder más allá de "minimo" */
const proyectar = (t: Trazado, x: number, y: number, minimo = 0): number => {
  let mejor = Infinity;
  let mejorS = Math.min(minimo, t.total);
  for (let i = 1; i < t.pts.length; i++) {
    const [ax, ay] = t.pts[i - 1];
    const [bx, by] = t.pts[i];
    const largo = t.acum[i] - t.acum[i - 1];
    if (largo === 0) continue;
    const u = Math.max(0, Math.min(1, ((x - ax) * (bx - ax) + (y - ay) * (by - ay)) / (largo * largo)));
    const sCand = t.acum[i - 1] + u * largo;
    if (sCand < minimo - 0.5) continue; // un vehículo no retrocede en su ruta
    const d = Math.hypot(ax + u * (bx - ax) - x, ay + u * (by - ay) - y);
    if (d < mejor) { mejor = d; mejorS = sCand; }
  }
  return mejorS;
};

/** Punto y ángulo de marcha (0° = este, 90° = norte) a una distancia dada del camino */
const puntoEn = (t: Trazado, s: number): { x: number; y: number; ang: number } => {
  const sc = Math.max(0, Math.min(t.total, s));
  let i = 1;
  while (i < t.pts.length - 1 && t.acum[i] < sc) i++;
  const [ax, ay] = t.pts[i - 1];
  const [bx, by] = t.pts[i];
  const largo = t.acum[i] - t.acum[i - 1] || 1;
  const u = (sc - t.acum[i - 1]) / largo;
  return { x: ax + u * (bx - ax), y: ay + u * (by - ay), ang: (Math.atan2(by - ay, bx - ax) * 180) / Math.PI };
};

// Si el reloj deja de llegar, no se adelanta más que esto (en tiempo real) para no inventar posiciones
const MAX_EXTRAPOLACION_MS = 10_000;

const limitar = (v: number, min: number, max: number) => Math.min(max, Math.max(min, v));

/** Código corto del pedido para la etiqueta del mapa: "PED-573-162fbe" → "573" (el completo está en el popup) */
const codigoCorto = (codigo: string) => {
  const m = /^[A-Za-z]+-(\d+)/.exec(codigo);
  return m ? m[1] : codigo.length > 8 ? codigo.slice(0, 8) : codigo;
};

// ===== Popup de detalle (clic en un vehículo, almacén o pedido) =====
type Seleccion = { tipo: 'vehiculo' | 'almacen' | 'pedido'; codigo: string };
const claveSel = (s: Seleccion) => `${s.tipo}:${s.codigo}`;

// Datos del enunciado por tipo de vehículo (se usan si el backend no manda el dato)
const DATOS_TIPO: Record<string, { velocidad: number; costoKm: number; capacidad: number }> = {
  Auto: { velocidad: 40, costoKm: 8, capacidad: 24 },
  Moto: { velocidad: 25, costoKm: 6, capacidad: 8 },
  Bicicleta: { velocidad: 12, costoKm: 3, capacidad: 4 },
};
const ESTADO_UNIDAD: Record<string, { texto: string; clase: string }> = {
  DISPONIBLE: { texto: 'Disponible', clase: 'bg-green-50 text-green-700 border-green-200' },
  EN_RUTA: { texto: 'En ruta', clase: 'bg-blue-50 text-blue-700 border-blue-200' },
  AVERIADA: { texto: 'Averiado', clase: 'bg-red-50 text-red-700 border-red-200' },
  EN_MANTENIMIENTO: { texto: 'Mantenimiento', clase: 'bg-amber-50 text-amber-700 border-amber-200' },
};
const CRITICIDAD: Record<string, { texto: string; clase: string }> = {
  VERDE: { texto: 'A tiempo', clase: 'bg-green-50 text-green-700 border-green-200' },
  AMBAR: { texto: 'En riesgo', clase: 'bg-amber-50 text-amber-700 border-amber-200' },
  ROJO: { texto: 'Crítico', clase: 'bg-red-50 text-red-700 border-red-200' },
};
const ESTADO_PEDIDO: Record<string, string> = {
  REGISTRADO: 'Por atender',
  PLANIFICADO: 'Planificado',
  EN_RUTA: 'En ruta',
  ENTREGADO: 'Entregado',
  REASIGNADO: 'Reasignado',
};

const InsigniaPopup: React.FC<{ texto: string; clase: string }> = ({ texto, clase }) => (
  <span className={`rounded-full border px-1.5 py-px text-[10px] font-semibold whitespace-nowrap ${clase}`}>{texto}</span>
);
const BarraPopup: React.FC<{ valor: number; total: number; color: string }> = ({ valor, total, color }) => (
  <div className="w-full h-1.5 rounded-full bg-gray-100 overflow-hidden">
    <div className="h-1.5 rounded-full" style={{ width: total > 0 ? `${Math.min(100, (valor / total) * 100)}%` : 0, backgroundColor: color }} />
  </div>
);
const FilaPopup: React.FC<{ etiqueta: string; children: React.ReactNode }> = ({ etiqueta, children }) => (
  <div className="flex items-center justify-between gap-3">
    <span className="text-gray-400">{etiqueta}</span>
    <span className="font-semibold text-gray-800 text-right">{children}</span>
  </div>
);

/** Lee el primer campo con valor entre varios nombres posibles (el backend puede llamarlos distinto) */
const campo = (obj: object, nombres: string[]): unknown => {
  const o = obj as Record<string, unknown>;
  for (const n of nombres) if (o[n] !== undefined && o[n] !== null && o[n] !== '') return o[n];
  return undefined;
};


export const MapaOperaciones: React.FC<MapaOperacionesProps> = ({
  almacenes,
  unidades,
  pedidos,
  bloqueos = [],
  rutas = [],
  selectedUnidadCodigo,
  onSelectUnidad,
  selectedAlmacenCodigo,
  onSelectAlmacen,
  selectedPedidoCodigo,
  onSelectPedido,
  escenario,
  pausado = false,
  reloj,
  filtros,
  averias = [],
  ocupacionGlobal,
}) => {
  const [showLeyenda, setShowLeyenda] = useState(false);

  // --- Popup de detalle: clic en un ícono lo abre; clic en el mismo, en la ✕, en el mapa vacío o Esc lo cierra ---
  const [seleccion, setSeleccion] = useState<Seleccion | null>(null);
  const popupRef = useRef<HTMLDivElement>(null);
  const contenedorRef = useRef<HTMLDivElement>(null);
  const huboArrastre = useRef(false); // un arrastre del mapa no debe abrir ni cerrar el popup
  const alternarSeleccion = (s: Seleccion) =>
    setSeleccion((prev) => (prev && claveSel(prev) === claveSel(s) ? null : s));
  const clicIcono = (e: React.MouseEvent, s: Seleccion) => {
    e.stopPropagation();
    if (huboArrastre.current) return;
    alternarSeleccion(s);
  };

  // Última posición y ángulo de cada vehículo: con eso se calcula hacia dónde se movió
  // y se mantiene su orientación cuando se detiene (como un auto estacionado).
  const memoriaMov = useRef(new Map<string, { x: number; y: number; ang: number }>());
  // Última posición vista de TODOS los vehículos (también los ocultos) y cuándo se movieron por última vez.
  // Sirve para mostrar en el mapa a los que se están moviendo aunque el backend no les marque ruta.
  const ultimoMovimiento = useRef(new Map<string, { x: number; y: number; t: number }>());

  // --- Animación sobre la ruta (se mueve el DOM directamente, sin volver a dibujar con React) ---
  const nodoVehiculo = useRef(new Map<string, SVGGElement>());   // grupo que se desplaza
  const giroVehiculo = useRef(new Map<string, SVGGElement>());   // grupo que gira
  const lineaRuta = useRef(new Map<string, SVGPathElement>());    // línea punteada de lo que le falta recorrer
  const recorrido = useRef(new Map<string, { clave: string; s: number }>()); // dónde se dibujó por última vez
  const animaciones = useRef(new Map<string, { t: Trazado; s0: number; s1: number; t0: number }>());
  const cuadro = useRef<number | null>(null);
  // Última posición que envió el backend de cada vehículo: si no cambió, el vehículo no se anima
  const ultimaPosBackend = useRef(new Map<string, string>());

  // Trazado de la ruta completa de cada vehículo (o, si no llegó el camino, la ruta aproximada)
  const trazados = useMemo(() => {
    const m = new Map<string, Trazado>();
    rutas.forEach((r) => {
      // Sin camino real, se arma la ruta aproximada en L (primero en X, luego en Y), igual que la línea dibujada
      const enL = (pts: Ubicacion[]) =>
        pts.flatMap((p, i) => (i === 0 ? [p] : [{ posX: p.posX, posY: pts[i - 1].posY }, p]));
      const base =
        r.caminoCompleto && r.caminoCompleto.length >= 2 ? r.caminoCompleto : r.esCamino ? r.puntos : enL(r.puntos);
      const clave = `${r.unidadCodigo}|${base.length}|${base[0]?.posX},${base[0]?.posY}|${base[base.length - 1]?.posX},${base[base.length - 1]?.posY}`;
      const t = crearTrazado(base, clave);
      if (t) m.set(r.unidadCodigo, t);
    });
    return m;
  }, [rutas]);

  // --- Desplazamiento arrastrando y zoom continuo, como en Google Maps ---
  const CENTRO_INICIAL = { x: BASE.x + BASE.w / 2, y: BASE.y + BASE.h / 2 };
  const [vista, setVista] = useState({ zoom: ESCALA, ...CENTRO_INICIAL });
  const vistaRef = useRef(vista);
  vistaRef.current = vista;
  const centro = vista;
  const svgRef = useRef<SVGSVGElement>(null);
  const arrastre = useRef<{ px: number; py: number; cx: number; cy: number; movido: boolean } | null>(null);
  const [arrastrando, setArrastrando] = useState(false);
  const animZoom = useRef<number | null>(null);

  // Unidades del mapa (km) por píxel de pantalla. Con preserveAspectRatio "slice" el mapa llena
  // todo el recuadro (sin franjas vacías), así que manda el lado con más píxeles por km.
  const kmPorPixel = (zoom = vistaRef.current.zoom) => {
    const el = svgRef.current;
    if (!el) return 0;
    const r = el.getBoundingClientRect();
    return Math.min(anchoVista(zoom) / r.width, altoVista(zoom) / r.height);
  };

  /** Mantiene la vista dentro de la ciudad usando lo que realmente se ve en pantalla
   *  (así se puede llegar a todos los bordes al arrastrar con zoom) */
  const ajustarCentro = (x: number, y: number, zoom: number) => {
    const r = svgRef.current?.getBoundingClientRect();
    const k = kmPorPixel(zoom);
    const visibleAncho = r && k ? r.width * k : anchoVista(zoom);
    const visibleAlto = r && k ? r.height * k : altoVista(zoom);
    return {
      x: visibleAncho >= BASE.w ? BASE.x + BASE.w / 2 : limitar(x, BASE.x + visibleAncho / 2, BASE.x + BASE.w - visibleAncho / 2),
      y: visibleAlto >= BASE.h ? BASE.y + BASE.h / 2 : limitar(y, BASE.y + visibleAlto / 2, BASE.y + BASE.h - visibleAlto / 2),
    };
  };

  /** Cambia el zoom dejando fijo el punto del mapa que está bajo el cursor (px, py) */
  const zoomEn = (nuevoZoom: number, px?: number, py?: number) => {
    const el = svgRef.current;
    if (!el) return;
    const v = vistaRef.current;
    const z = limitar(nuevoZoom, ZOOM_MIN, ZOOM_MAX);
    const r = el.getBoundingClientRect();
    const ox = (px ?? r.left + r.width / 2) - (r.left + r.width / 2);
    const oy = (py ?? r.top + r.height / 2) - (r.top + r.height / 2);
    const dk = kmPorPixel(v.zoom) - kmPorPixel(z);
    setVista({ zoom: z, ...ajustarCentro(v.x + ox * dk, v.y + oy * dk, z) });
  };

  /** Zoom de los botones: avanza poco a poco durante 250 ms, sin saltos */
  const zoomAnimado = (objetivo: number) => {
    if (animZoom.current !== null) cancelAnimationFrame(animZoom.current);
    const inicio = performance.now();
    const z0 = vistaRef.current.zoom;
    const z1 = limitar(objetivo, ZOOM_MIN, ZOOM_MAX);
    const paso = (t: number) => {
      const f = Math.min(1, (t - inicio) / 250);
      zoomEn(z0 * Math.pow(z1 / z0, 1 - Math.pow(1 - f, 3)));
      animZoom.current = f < 1 ? requestAnimationFrame(paso) : null;
    };
    animZoom.current = requestAnimationFrame(paso);
  };

  /** Vuelve a la vista inicial */
  const vistaInicial = () => {
    if (animZoom.current !== null) cancelAnimationFrame(animZoom.current);
    setVista({ zoom: ESCALA, ...CENTRO_INICIAL });
  };

  // Al abrir o cerrar el panel cambia el tamaño del mapa: se reacomoda la vista
  useEffect(() => {
    const el = svgRef.current;
    if (!el || typeof ResizeObserver === 'undefined') return;
    const observador = new ResizeObserver(() => {
      const v = vistaRef.current;
      setVista({ zoom: v.zoom, ...ajustarCentro(v.x, v.y, v.zoom) });
    });
    observador.observe(el);
    return () => observador.disconnect();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Rueda del mouse: se registra a mano (passive: false) para que la página no haga scroll
  useEffect(() => {
    const el = svgRef.current;
    if (!el) return;
    const alGirar = (e: WheelEvent) => {
      e.preventDefault();
      // Continuo: el zoom cambia en proporción a cuánto gira la rueda
      zoomEn(vistaRef.current.zoom * Math.exp(-e.deltaY * 0.0015), e.clientX, e.clientY);
    };
    el.addEventListener('wheel', alGirar, { passive: false });
    return () => el.removeEventListener('wheel', alGirar);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const alPresionar = (e: React.PointerEvent<SVGSVGElement>) => {
    huboArrastre.current = false;
    arrastre.current = { px: e.clientX, py: e.clientY, cx: centro.x, cy: centro.y, movido: false };
  };

  const alMover = (e: React.PointerEvent<SVGSVGElement>) => {
    const a = arrastre.current;
    if (!a) return;
    const dx = e.clientX - a.px;
    const dy = e.clientY - a.py;
    if (!a.movido && Math.hypot(dx, dy) < 4) return; // pequeño umbral: un clic no mueve el mapa
    if (!a.movido) {
      a.movido = true;
      huboArrastre.current = true;
      setArrastrando(true);
      e.currentTarget.setPointerCapture(e.pointerId);
    }
    const k = kmPorPixel();
    const z = vistaRef.current.zoom;
    setVista({ zoom: z, ...ajustarCentro(a.cx - dx * k, a.cy - dy * k, z) });
  };

  const alSoltar = () => {
    arrastre.current = null;
    setArrastrando(false);
  };

  const ANCHO_VISTA = anchoVista(vista.zoom);
  const ALTO_VISTA = altoVista(vista.zoom);
  const viewBox = `${centro.x - ANCHO_VISTA / 2} ${centro.y - ALTO_VISTA / 2} ${ANCHO_VISTA} ${ALTO_VISTA}`;

  // En el mapa se dibujan los pedidos ya asignados a un vehículo: planificados, en ruta, o que
  // figuran como parada pendiente de alguna ruta. Así toda línea de ruta termina en un punto.
  // (El panel derecho sigue contando todos los pedidos.)
  const listaRutas = Array.isArray(rutas) ? rutas : [];
  const listaPedidos = Array.isArray(pedidos) ? pedidos : [];
  const codigosEnRuta = new Set(listaRutas.flatMap((r) => r.codigosPedidos ?? []));
  const pedidosVisibles = listaPedidos.filter(
    (p) => p && p.destino && (p.estado === 'PLANIFICADO' || p.estado === 'EN_RUTA' || codigosEnRuta.has(p.codigo)),
  );

  // La matriz tiene 70 de ancho por 50 de alto: 1 unidad SVG = 1 km.
  // El origen del backend está abajo a la izquierda y el de SVG arriba a la izquierda:
  const toSvgX = (x: number) => x;
  const toSvgY = (y: number) => 50 - y;

  // ---- Movimiento continuo: horario de cada ruta + reloj simulado estimado entre consultas ----
  const horarios = useMemo(() => {
    const m = new Map<string, Horario>();
    rutas.forEach((r) => {
      const tr = trazados.get(r.unidadCodigo);
      // Los índices del horario apuntan a caminoCompleto, que es el camino del trazado
      if (!tr || !r.horario || !r.caminoCompleto || r.caminoCompleto.length < 2) return;
      const h = crearHorario(tr, r.horario);
      if (h) m.set(r.unidadCodigo, h);
    });
    return m;
  }, [rutas, trazados]);

  const instanteMs = parsearFechaLocal(reloj?.instanteActual);
  const factorReloj = reloj?.factorAceleracion && reloj.factorAceleracion > 0 ? reloj.factorAceleracion : 1;
  const relojCorriendo = !!reloj?.corriendo && !pausado;
  const relojDisponible = instanteMs !== null;

  // Última hora simulada recibida y en qué momento (real) llegó; entre consultas se extrapola
  const relojRef = useRef<{ ms: number; recibido: number; factor: number; corriendo: boolean } | null>(null);
  const mostradoRef = useRef<number | null>(null);
  useLayoutEffect(() => {
    if (instanteMs === null) {
      relojRef.current = null;
      mostradoRef.current = null;
      return;
    }
    relojRef.current = { ms: instanteMs, recibido: performance.now(), factor: factorReloj, corriendo: relojCorriendo };
  }, [instanteMs, factorReloj, relojCorriendo]);

  /** Hora simulada que se muestra ahora: estimada entre consultas y corregida con suavidad (sin congelarse) */
  const ultimoCuadroRef = useRef<number | null>(null);
  const simAhora = (ahora: number): number | null => {
    const r = relojRef.current;
    if (!r) return null;
    const objetivo = r.ms + (r.corriendo ? Math.min(ahora - r.recibido, MAX_EXTRAPOLACION_MS) * r.factor : 0);
    const dt = ultimoCuadroRef.current === null ? 0 : ahora - ultimoCuadroRef.current;
    ultimoCuadroRef.current = ahora;
    const ms = suavizarReloj(mostradoRef.current, objetivo, dt, r.corriendo, r.factor);
    mostradoRef.current = ms;
    return ms;
  };

  const horariosRef = useRef(horarios);
  horariosRef.current = horarios;
  const trazadosRef = useRef(trazados);
  trazadosRef.current = trazados;
  const averiadosRef = useRef(new Set<string>());
  averiadosRef.current = new Set(
    (Array.isArray(unidades) ? unidades : []).filter((u) => u.estadoOperativo === 'AVERIADA').map((u) => u.codigo),
  );

  /** Línea de lo que le falta recorrer: desde donde está dibujado el vehículo hasta el final del camino */
  const caminoRestante = (tr: Trazado, s: number, x: number, y: number): string => {
    const partes = [`M ${toSvgX(x)} ${toSvgY(y)}`];
    for (let i = 0; i < tr.pts.length; i++) {
      if (tr.acum[i] > s + 1e-6) partes.push(`L ${toSvgX(tr.pts[i][0])} ${toSvgY(tr.pts[i][1])}`);
    }
    return partes.join(' ');
  };

  /** Ubica cada vehículo con horario en su posición para la hora simulada actual */
  const dibujarContinuo = (ahora: number) => {
    const ms = simAhora(ahora);
    if (ms === null) return;
    const msVisible = sinAlimentacion(ms);
    horariosRef.current.forEach((h, codigo) => {
      if (averiadosRef.current.has(codigo)) return;
      const tr = trazadosRef.current.get(codigo);
      const nodo = nodoVehiculo.current.get(codigo);
      if (!tr || !nodo) return;
      const s = distanciaEn(h, msVisible);
      const { x, y, ang } = puntoEn(tr, s);
      nodo.setAttribute('transform', `translate(${toSvgX(x)} ${toSvgY(y)})`);
      giroVehiculo.current.get(codigo)?.setAttribute('transform', `rotate(${-ang})`);
      lineaRuta.current.get(codigo)?.setAttribute('d', caminoRestante(tr, s, x, y));
      recorrido.current.set(codigo, { clave: tr.clave, s });
    });
  };
  const dibujarContinuoRef = useRef(dibujarContinuo);
  dibujarContinuoRef.current = dibujarContinuo;

  // Un cuadro por refresco de pantalla mientras el mapa esté abierto
  useEffect(() => {
    let id = 0;
    const paso = (momento: number) => {
      dibujarContinuoRef.current(momento);
      id = requestAnimationFrame(paso);
    };
    id = requestAnimationFrame(paso);
    return () => cancelAnimationFrame(id);
  }, []);

  // Tras cada render se ubican de inmediato, para que nadie aparezca un instante en otro lugar
  useLayoutEffect(() => {
    dibujarContinuoRef.current(performance.now());
  });

  // Respaldo para vehículos sin horario (o sin reloj): cada vez que llegan posiciones nuevas, recorre
  // su camino desde donde está dibujado hasta su nueva posición, en lo que tarda la siguiente consulta.
  useLayoutEffect(() => {
    const ahora = performance.now();
    (Array.isArray(unidades) ? unidades : []).forEach((u) => {
      const t = trazados.get(u.codigo);
      if (!t || u.estadoOperativo === 'AVERIADA') {
        animaciones.current.delete(u.codigo);
        recorrido.current.delete(u.codigo);
        ultimaPosBackend.current.delete(u.codigo);
        return;
      }
      if (relojDisponible && horarios.has(u.codigo)) {
        // Lo mueve el movimiento continuo
        animaciones.current.delete(u.codigo);
        ultimaPosBackend.current.delete(u.codigo);
        return;
      }
      const previo = recorrido.current.get(u.codigo);
      const mismaRuta = previo?.clave === t.clave;

      // Posición enviada por el backend: si es la misma que la vez anterior (por ejemplo, en pausa),
      // el vehículo se queda quieto donde está dibujado en vez de seguir avanzando
      const posBackend = `${u.ubicacionActual.posX},${u.ubicacionActual.posY}`;
      const sinCambio = ultimaPosBackend.current.get(u.codigo) === posBackend;
      ultimaPosBackend.current.set(u.codigo, posBackend);

      if (mismaRuta && (pausado || sinCambio)) {
        animaciones.current.set(u.codigo, { t, s0: previo!.s, s1: previo!.s, t0: ahora });
        return;
      }
      const sDestino = proyectar(t, u.ubicacionActual.posX, u.ubicacionActual.posY, mismaRuta ? previo!.s : 0);
      const sInicio = mismaRuta ? previo!.s : sDestino; // ruta nueva: aparece directamente en su lugar
      animaciones.current.set(u.codigo, { t, s0: sInicio, s1: Math.max(sInicio, sDestino), t0: ahora });
    });

    const dibujar = (momento: number) => {
      animaciones.current.forEach((a, codigo) => {
        const f = Math.min(1, (momento - a.t0) / DURACION_MOVIMIENTO_MS);
        const s = a.s0 + (a.s1 - a.s0) * f;
        const { x, y, ang } = puntoEn(a.t, s);
        nodoVehiculo.current.get(codigo)?.setAttribute('transform', `translate(${toSvgX(x)} ${toSvgY(y)})`);
        giroVehiculo.current.get(codigo)?.setAttribute('transform', `rotate(${-ang})`);
        lineaRuta.current.get(codigo)?.setAttribute('d', caminoRestante(a.t, s, x, y));
        recorrido.current.set(codigo, { clave: a.t.clave, s });
        if (f >= 1) animaciones.current.delete(codigo);
      });
      cuadro.current = animaciones.current.size > 0 ? requestAnimationFrame(dibujar) : null;
    };

    // Primer cuadro inmediato (antes de pintar) para que nadie aparezca en la esquina del mapa
    dibujar(ahora);
    if (cuadro.current === null && animaciones.current.size > 0) cuadro.current = requestAnimationFrame(dibujar);
    return () => {
      if (cuadro.current !== null) cancelAnimationFrame(cuadro.current);
      cuadro.current = null;
    };
  }, [unidades, trazados, pausado, horarios, relojDisponible]);

  // ---- Qué vehículos se dibujan ----
  // Solo los que tienen trabajo: con ruta, en ruta, que se movieron hace poco o averiados (y el
  // seleccionado). Los que esperan sin trabajo en el almacén no se dibujan, para no amontonar íconos;
  // su cantidad se ve en el panel de Vehículos.
  const rutaDe = new Map(rutas.map((r) => [r.unidadCodigo, r]));
  const ahoraMs = Date.now();
  const VENTANA_MOVIMIENTO_MS = 60000; // sigue visible un rato tras moverse (p. ej. mientras entrega)
  const conTrabajo = new Set<string>();
  unidades.forEach((u) => {
    const ult = ultimoMovimiento.current.get(u.codigo);
    const cambio = !!ult && (ult.x !== u.ubicacionActual.posX || ult.y !== u.ubicacionActual.posY);
    const t = cambio ? ahoraMs : ult?.t ?? 0;
    ultimoMovimiento.current.set(u.codigo, { x: u.ubicacionActual.posX, y: u.ubicacionActual.posY, t });
    if (
      rutaDe.has(u.codigo) ||
      u.estadoOperativo === 'EN_RUTA' ||
      u.estadoOperativo === 'AVERIADA' ||
      ahoraMs - t < VENTANA_MOVIMIENTO_MS ||
      u.codigo === selectedUnidadCodigo
    ) {
      conTrabajo.add(u.codigo);
    }
  });
  const visibles = SOLO_VEHICULOS_CON_TRABAJO ? unidades.filter((u) => conTrabajo.has(u.codigo)) : unidades;

  // Tipo de avería de cada vehículo: del propio vehículo si el backend lo manda; si no, de la lista de averías
  const tipoAveriaDe = (u: UnidadTransporte): string => tipoAveriaInformado(u) ?? '1';
  const tipoAveriaInformado = (u: UnidadTransporte): string | null => {
    const propio = numeroTipoAveria(campo(u, ['tipoAveria', 'tipoAveriaActual', 'averiaTipo', 'tipoAveriaActiva']));
    if (propio) return propio;
    const registrada = [...averias]
      .reverse()
      .find((a) => (a.idUnidad ?? a.unidadCodigo) === u.codigo && a.activa !== false);
    return registrada ? numeroTipoAveria(registrada.tipo ?? registrada.tipoAveria) : null;
  };

  // Esc cierra el popup
  useEffect(() => {
    if (!seleccion) return;
    const alTeclear = (e: KeyboardEvent) => { if (e.key === 'Escape') setSeleccion(null); };
    window.addEventListener('keydown', alTeclear);
    return () => window.removeEventListener('keydown', alTeclear);
  }, [seleccion]);

  // El popup sigue a su ícono en cada cuadro (el vehículo se mueve, y el mapa se arrastra o hace zoom)
  useEffect(() => {
    if (!seleccion) return;
    let cuadroPopup = 0;
    const seguir = () => {
      const svg = svgRef.current;
      const cont = contenedorRef.current;
      const pop = popupRef.current;
      const el = svg?.querySelector(`[data-sel="${claveSel(seleccion)}"]`) as SVGGraphicsElement | null;
      if (cont && pop) {
        if (!el) {
          pop.style.visibility = 'hidden'; // por ejemplo, si se ocultó con los filtros
        } else {
          const rc = cont.getBoundingClientRect();
          const ri = el.getBoundingClientRect();
          const ancho = pop.offsetWidth;
          const alto = pop.offsetHeight;
          // A la derecha del ícono; si no entra, a la izquierda. Siempre dentro del mapa.
          let x = ri.right - rc.left + 10;
          if (x + ancho > rc.width - 8) x = ri.left - rc.left - ancho - 10;
          const y = limitar(ri.top - rc.top + ri.height / 2 - alto / 2, 8, rc.height - alto - 8);
          pop.style.transform = `translate(${Math.max(8, x)}px, ${y}px)`;
          pop.style.visibility = 'visible';
        }
      }
      cuadroPopup = requestAnimationFrame(seguir);
    };
    cuadroPopup = requestAnimationFrame(seguir);
    return () => cancelAnimationFrame(cuadroPopup);
  }, [seleccion]);

  // ---- Registrar una avería desde el mapa (FAQ 3: las averías se registran en el visualizador) ----
  const [registrandoAveria, setRegistrandoAveria] = useState(false);
  const [avisoAveria, setAvisoAveria] = useState<{ codigo: string; texto: string; error: boolean } | null>(null);

  /** Esquina en la que el operario ve al vehículo: ahí queda detenido por la avería */
  const posicionDibujada = (u: UnidadTransporte): Ubicacion => {
    const tr = trazados.get(u.codigo);
    const rec = recorrido.current.get(u.codigo);
    if (tr && rec && rec.clave === tr.clave) {
      const { x, y } = puntoEn(tr, rec.s);
      return { posX: Math.round(x), posY: Math.round(y) };
    }
    return u.ubicacionActual;
  };

  const registrarAveria = async (u: UnidadTransporte, n: '1' | '2' | '3') => {
    setRegistrandoAveria(true);
    try {
      await averiasApi.registrar({ idUnidad: u.codigo, tipo: `TIPO_${n}`, ubicacion: posicionDibujada(u) });
      setAvisoAveria({ codigo: u.codigo, texto: `Avería tipo ${n} registrada. ${AVERIA_TIPO[n].detalle}.`, error: false });
    } catch (err: unknown) {
      const resp = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setAvisoAveria({ codigo: u.codigo, texto: resp ?? 'No se pudo registrar la avería.', error: true });
    } finally {
      setRegistrandoAveria(false);
    }
  };

  /**
   * Si el vehículo está detenido en una parada según su horario: la hora de acondicionamiento en el
   * cliente (FAQ 14) o el trasvase de una ruta de rescate. Usa la misma hora simulada que se dibuja.
   */
  const actividadEnParada = (r?: RutaMapa): string | null => {
    const ms = mostradoRef.current;
    const h = r?.horario;
    if (ms === null || !h) return null;
    for (let i = 0; i < h.paradas.length; i++) {
      const p = h.paradas[i];
      if (ms >= p.llegada && ms < p.salida) {
        return i === 0 && r?.rescataA ? `Trasvasando la carga de ${r.rescataA}` : 'Entregando producto';
      }
    }
    return null;
  };

  // ---- Contenido del popup según lo seleccionado ----
  const contenidoPopup = (() => {
    if (!seleccion) return null;
    if (seleccion.tipo === 'vehiculo') {
      const u = unidades.find((x) => x.codigo === seleccion.codigo);
      if (!u) return null;
      const d = DATOS_TIPO[u.tipoNombre];
      const capacidad = u.capacidadMaxima || d?.capacidad || 0;
      const pendientes = rutaDe.get(u.codigo)?.codigosPedidos ?? [];
      const est = ESTADO_UNIDAD[u.estadoOperativo] ?? { texto: u.estadoOperativo, clase: 'bg-gray-50 text-gray-700 border-gray-200' };
      return {
        color: COLOR_TIPO[u.tipoNombre] ?? COLOR_TIPO.Bicicleta,
        titulo: `${u.tipoNombre} ${u.codigo}`,
        insignia: <InsigniaPopup texto={est.texto} clase={est.clase} />,
        cuerpo: (
          <>
            <div>
              <div className="flex justify-between mb-1">
                <span className="text-gray-400">Carga</span>
                <span className="font-semibold text-gray-800">{u.cargaActual}/{capacidad} paquetes</span>
              </div>
              <BarraPopup valor={u.cargaActual} total={capacidad} color={COLOR_TIPO[u.tipoNombre] ?? '#3B82F6'} />
            </div>
            <FilaPopup etiqueta="Pendientes">
              {(() => {
                const actividad = actividadEnParada(rutaDe.get(u.codigo));
                if (actividad) return pendientes.length > 0 ? `${actividad} · quedan ${pendientes.length}` : actividad;
                if (pendientes.length > 0) return `${pendientes.length} · sigue ${pendientes[0]}`;
                return rutaDe.has(u.codigo) ? 'Regresando al almacén' : 'Sin ruta asignada';
              })()}
            </FilaPopup>
            {rutaDe.get(u.codigo)?.rescataA && (
              <FilaPopup etiqueta="Rescate">
                <span className="font-semibold text-red-700">Trasvase de la carga de {rutaDe.get(u.codigo)?.rescataA}</span>
              </FilaPopup>
            )}
            {u.estadoOperativo === 'AVERIADA' && (() => {
              const info = AVERIA_TIPO[tipoAveriaDe(u)];
              return (
                <FilaPopup etiqueta="Avería">
                  <span className="inline-flex items-center gap-1">
                    <span className="w-2 h-2 rounded-full" style={{ backgroundColor: info.color }} />
                    {info.nombre} · {info.riesgo}
                  </span>
                </FilaPopup>
              );
            })()}
            <FilaPopup etiqueta="Ubicación">({u.ubicacionActual.posX}, {u.ubicacionActual.posY})</FilaPopup>
            {u.estadoOperativo !== 'AVERIADA' && u.estadoOperativo !== 'EN_MANTENIMIENTO' && (
              <div className="pt-1">
                <div className="text-gray-400 mb-1">Registrar avería</div>
                <div className="flex gap-1.5">
                  {(['1', '2', '3'] as const).map((n) => (
                    <button
                      key={n}
                      type="button"
                      disabled={registrandoAveria}
                      title={`${AVERIA_TIPO[n].nombre}: ${AVERIA_TIPO[n].detalle}`}
                      onClick={(e) => { e.stopPropagation(); void registrarAveria(u, n); }}
                      className="flex-1 inline-flex items-center justify-center gap-1 rounded-md border border-gray-200 px-2 py-1 font-semibold text-gray-700 hover:bg-gray-50 disabled:opacity-50"
                    >
                      <svg viewBox="-0.6 -0.6 1.2 1.05" className="w-3.5 h-3.5" aria-hidden="true">
                        <polygon points="0,-0.55 0.5,0.36 -0.5,0.36" fill={AVERIA_TIPO[n].color} stroke="#111827" strokeWidth="0.07" />
                      </svg>
                      Tipo {n}
                    </button>
                  ))}
                </div>
              </div>
            )}
            {avisoAveria?.codigo === u.codigo && (
              <p className={`text-[11px] ${avisoAveria.error ? 'text-red-600' : 'text-green-700'}`}>{avisoAveria.texto}</p>
            )}
            <FilaPopup etiqueta="Velocidad · costo">
              {u.velocidadPromedioKmH || d?.velocidad} km/h · S/ {u.costoPorKm || d?.costoKm}/km
            </FilaPopup>
          </>
        ),
      };
    }
    if (seleccion.tipo === 'almacen') {
      const a = almacenes.find((x) => x.codigo === seleccion.codigo);
      if (!a) return null;
      const esNorte = /n[-\s]?o/i.test(a.nombre) || a.nombre.toLowerCase().includes('norte');
      const color = a.tipo === 'CENTRAL' ? COLOR_ALMACEN.central : esNorte ? COLOR_ALMACEN.norte : COLOR_ALMACEN.este;
      const ilimitado = a.tipo === 'CENTRAL' || a.stockActual === null || a.stockActual === undefined;
      const capacidad = a.capacidadMaxima ?? 1000;
      return {
        color,
        titulo: a.nombre,
        insignia: <InsigniaPopup texto={a.tipo === 'CENTRAL' ? 'Central' : 'Intermedio'} clase="bg-gray-50 text-gray-700 border-gray-200" />,
        cuerpo: ilimitado ? (
          <FilaPopup etiqueta="Stock">∞ Ilimitado</FilaPopup>
        ) : (
          <>
            <div>
              <div className="flex justify-between mb-1">
                <span className="text-gray-400">Stock</span>
                <span className="font-semibold text-gray-800">{a.stockActual}/{capacidad}</span>
              </div>
              <BarraPopup valor={a.stockActual ?? 0} total={capacidad} color={color} />
            </div>
            <FilaPopup etiqueta="Recarga">Diaria, 23:59</FilaPopup>
          </>
        ),
      };
    }
    // Pedido
    const p = pedidos.find((x) => x.codigo === seleccion.codigo);
    if (!p) return null;
    const cri = CRITICIDAD[p.criticidad] ?? { texto: p.criticidad, clase: 'bg-gray-50 text-gray-700 border-gray-200' };
    const asignado = rutas.find((r) => (r.codigosPedidos ?? []).includes(p.codigo))?.unidadCodigo;
    const cantidad = campo(p, ['cantidad', 'cantidadPaquetes', 'cantidadProductos', 'numPaquetes']);
    const limite = campo(p, ['fechaHoraLimite', 'fechaLimite', 'plazoLimite', 'horaLimite', 'fechaEntregaLimite']);
    const cliente = campo(p, ['clienteNombre', 'cliente', 'idCliente', 'codigoCliente']);
    return {
      color: COLOR_CRITICIDAD[p.criticidad] ?? '#2563EB',
      titulo: `Pedido ${p.codigo}`,
      insignia: <InsigniaPopup texto={cri.texto} clase={cri.clase} />,
      cuerpo: (
        <>
          <FilaPopup etiqueta="Estado">{ESTADO_PEDIDO[p.estado] ?? p.estado}</FilaPopup>
          {cantidad !== undefined && <FilaPopup etiqueta="Cantidad">{String(cantidad)} paquetes</FilaPopup>}
          {limite !== undefined && (
            <FilaPopup etiqueta="Plazo límite">
              {new Date(String(limite)).toLocaleString('es-PE', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })}
            </FilaPopup>
          )}
          {cliente !== undefined && <FilaPopup etiqueta="Cliente">{String(cliente)}</FilaPopup>}
          <FilaPopup etiqueta="Destino">({p.destino.posX}, {p.destino.posY})</FilaPopup>
          <FilaPopup etiqueta="Asignado a">{asignado ?? 'Sin asignar'}</FilaPopup>
        </>
      ),
    };
  })();

  return (
    <div ref={contenedorRef} className="relative w-full h-full min-h-[480px] bg-[#f8fafc] rounded-xl border border-gray-200 overflow-hidden">
      <svg
        ref={svgRef}
        className={`w-full h-full bg-[#f8fafc] touch-none ${arrastrando ? 'cursor-grabbing' : 'cursor-grab'}`}
        viewBox={viewBox}
        preserveAspectRatio="xMidYMid slice"
        onPointerDown={alPresionar}
        onPointerMove={alMover}
        onPointerUp={alSoltar}
        onPointerLeave={alSoltar}
        onClick={() => { if (!huboArrastre.current) setSeleccion(null); }}
      >
        {/* Sombra suave para que pines, vehículos y almacenes resalten sobre la cuadrícula */}
        <defs>
          {/* En proporción al tamaño de cada ícono, así sirve igual para pines, vehículos y almacenes */}
          <filter id="sombraIcono" x="-60%" y="-60%" width="220%" height="220%" primitiveUnits="objectBoundingBox">
            <feDropShadow dx="0" dy="0.05" stdDeviation="0.04" floodColor="#0F172A" floodOpacity="0.35" />
          </filter>
        </defs>

        {/* Cuadros de relleno fuera de la ciudad, más tenues, para que los bordes no queden
            en blanco y aun así se distinga dónde termina el área de 70 × 50 km */}
        <g stroke="#E2E8F0" strokeWidth="0.05">
          {Array.from({ length: 111 }).map((_, i) => (
            <line key={`ev-${i}`} x1={i - 20} y1={-20} x2={i - 20} y2={70} />
          ))}
          {Array.from({ length: 91 }).map((_, i) => (
            <line key={`eh-${i}`} x1={-20} y1={i - 20} x2={90} y2={i - 20} />
          ))}
        </g>

        {/* Cuadrícula de la ciudad: una línea cada 1 km */}
        <g stroke="#CBD5E1" strokeWidth="0.05">
          {Array.from({ length: 71 }).map((_, i) => (
            <line key={`v-${i}`} x1={i} y1={0} x2={i} y2={50} />
          ))}
          {Array.from({ length: 51 }).map((_, i) => (
            <line key={`h-${i}`} x1={0} y1={i} x2={70} y2={i} />
          ))}
        </g>

        {/* Rutas planificadas: punteadas con el color de su vehículo (azul auto, morado moto, rosa bici).
            La del vehículo seleccionado se ve más gruesa y las demás se atenúan. */}
        {rutas.map((r) => {
          if (r.puntos.length < 2) return null;
          const unidadRuta = unidades.find((u) => u.codigo === r.unidadCodigo);
          if (unidadRuta && !pasaFiltroUnidad(unidadRuta, filtros)) return null; // vehículo oculto con los filtros
          const colorRuta = unidadRuta ? COLOR_TIPO[unidadRuta.tipoNombre] ?? '#6B7280' : '#6B7280';
          const codigoSel = seleccion?.tipo === 'vehiculo' ? seleccion.codigo : selectedUnidadCodigo;
          const contienePedidoSel = !!selectedPedidoCodigo && (r.codigosPedidos?.includes(selectedPedidoCodigo) ?? false);
          const esSel = (codigoSel === r.unidadCodigo) || contienePedidoSel;
          const hayOtraSel = (!!codigoSel && !esSel) || (!!selectedPedidoCodigo && !contienePedidoSel);
          let d: string;
          const trazado = trazados.get(r.unidadCodigo);
          const dibujado = recorrido.current.get(r.unidadCodigo);
          if (trazado && dibujado && dibujado.clave === trazado.clave) {
            // Pegada al vehículo: desde donde está dibujado hasta el final de su camino
            const { x, y } = puntoEn(trazado, dibujado.s);
            d = caminoRestante(trazado, dibujado.s, x, y);
          } else if (r.esCamino) {
            // El backend ya entrega esquina por esquina esquivando bloqueos, por eso basta unirlas
            d = r.puntos.map((pt, i) => `${i === 0 ? 'M' : 'L'} ${toSvgX(pt.posX)} ${toSvgY(pt.posY)}`).join(' ');
          } else {
            // Si la ruta llega sin camino, se aproxima en forma de L para no dejar de mostrarla
            const tramos: string[] = [];
            r.puntos.forEach((pt, i) => {
              const x = toSvgX(pt.posX);
              const y = toSvgY(pt.posY);
              if (i === 0) {
                tramos.push(`M ${x} ${y}`);
              } else {
                const prevY = toSvgY(r.puntos[i - 1].posY);
                tramos.push(`L ${x} ${prevY} L ${x} ${y}`);
              }
            });
            d = tramos.join(' ');
          }
          return (
            <path
              key={`ruta-${r.unidadCodigo}`}
              ref={(el) => {
                if (el) lineaRuta.current.set(r.unidadCodigo, el);
                else lineaRuta.current.delete(r.unidadCodigo);
              }}
              d={d}
              fill="none"
              stroke={contienePedidoSel ? '#2563EB' : colorRuta}
              strokeOpacity={esSel ? 1 : hayOtraSel ? 0.2 : 0.6}
              strokeWidth={contienePedidoSel ? 0.26 : esSel ? 0.22 : 0.13}
              strokeDasharray={contienePedidoSel ? undefined : '0.45 0.3'}
              strokeLinecap="round"
              strokeLinejoin="round"
              className="pointer-events-none"
            />
          );
        })}

        {/* Bloqueos de calle activos: tramo en negro con una barrera al medio */}
        {(Array.isArray(bloqueos) ? bloqueos : [])
          .filter((b) => b && b.activo)
          .map((b) => {
            const pts = parsePuntos(b.coordenadasNodos);
            if (pts.length < 2) return null;
            const linea = pts.map(([x, y]) => `${toSvgX(x)},${toSvgY(y)}`).join(' ');
            const medio = pts[Math.floor(pts.length / 2)] as [number, number];
            const mx = toSvgX(medio[0]);
            const my = toSvgY(medio[1]);
            return (
              <g key={`bloqueo-${b.codigo ?? b.id}`} className="select-none">
                <polyline points={linea} fill="none" stroke="#111827" strokeWidth="0.2" strokeLinecap="round" strokeLinejoin="round" />
                {/* Barrera de obra: blanca con franjas rojas y borde negro, como las barreras reales */}
                <rect x={mx - 0.45} y={my - 0.28} width="0.9" height="0.56" rx="0.1" fill="white" stroke="#111827" strokeWidth="0.07" />
                <path
                  d={`M ${mx - 0.3} ${my + 0.22} L ${mx - 0.05} ${my - 0.22} M ${mx + 0.05} ${my + 0.22} L ${mx + 0.3} ${my - 0.22}`}
                  stroke="#DC2626"
                  strokeWidth="0.12"
                />
                <text x={mx + 0.65} y={my - 0.35} fontSize="0.5" fontWeight="bold" fill="#111827">Bloqueo</text>
              </g>
            );
          })}

        {/* Pedidos: pin de ubicación con una caja; el color indica su criticidad (semáforo).
            La punta del pin marca el punto exacto de entrega. */}
        {pedidosVisibles.map((p) => {
          const px = toSvgX(p.destino.posX);
          const py = toSvgY(p.destino.posY);
          const color = COLOR_CRITICIDAD[p.criticidad] ?? '#2563EB';
            const esPedidoSel =
              (seleccion?.tipo === 'pedido' && seleccion.codigo === p.codigo) ||
              selectedPedidoCodigo === p.codigo;
            return (
              <g
                key={`pedido-${p.id}`}
                data-sel={`pedido:${p.codigo}`}
                className="cursor-pointer"
                onClick={(e) => {
                  onSelectPedido?.(p.codigo);
                  clicIcono(e, { tipo: 'pedido', codigo: p.codigo });
                }}
              >
                <title>{`Pedido ${p.codigo}`}</title>
                {esPedidoSel && (
                  <circle cx={px} cy={py - TAMANO_ICONO * 0.6} r={TAMANO_ICONO * 0.85} fill="none" stroke="#2563EB" strokeWidth="0.18" className="animate-pulse" />
                )}
              <g transform={`translate(${px}, ${py}) scale(${ESCALA_PIN})`} filter="url(#sombraIcono)">
                <PinPedido color={color} />
              </g>
              <text
                x={px}
                y={py - TAMANO_ICONO - 0.15}
                textAnchor="middle"
                fontSize="0.4"
                fontWeight="bold"
                fill="#334155"
                stroke="white"
                strokeWidth="0.12"
                paintOrder="stroke"
                className="select-none"
              >
                {vista.zoom >= 3 ? p.codigo : codigoCorto(p.codigo)}
              </text>
            </g>
          );
        })}

        {/* Almacenes: bodega con su letra, semáforo de ocupación y texto de stock */}
        {almacenes.filter((alm) => pasaFiltroAlmacen(alm, filtros)).map((alm) => {
          const ax = toSvgX(alm.ubicacion.posX);
          const ay = toSvgY(alm.ubicacion.posY);
          const esNoroeste = /n[-\s]?o/i.test(alm.nombre) || alm.nombre.toLowerCase().includes('norte');
          const letra = alm.tipo === 'CENTRAL' ? 'C' : esNoroeste ? 'N' : 'E';
          const color = alm.tipo === 'CENTRAL' ? COLOR_ALMACEN.central : esNoroeste ? COLOR_ALMACEN.norte : COLOR_ALMACEN.este;
          const isCentral = alm.tipo === 'CENTRAL';
          const capAlm = alm.capacidadMaxima || 1000;
          const stockAlm = alm.stockActual ?? (isCentral ? 9999 : 0);
          const pctAlm = isCentral ? 100 : Math.min(100, Math.round((stockAlm / capAlm) * 100));
          const semAlm = nivelSemaforoAlmacen(alm);
          const semAlmColor = isCentral
            ? '#EA580C'
            : semAlm === 'ROJO'
            ? '#EF4444'
            : semAlm === 'AMBAR'
            ? '#F59E0B'
            : semAlm === 'VERDE'
            ? '#22C55E'
            : '#9CA3AF';

          const esSel =
            (seleccion?.tipo === 'almacen' && seleccion.codigo === alm.codigo) ||
            selectedAlmacenCodigo === alm.codigo;

          const tooltip = isCentral
            ? `${alm.nombre} · Stock Ilimitado`
            : `${alm.nombre} · Stock: ${stockAlm}/${capAlm} u (${pctAlm}%) · Semáforo: ${semAlm}`;

          return (
            <g
              key={`alm-${alm.codigo}`}
              data-sel={`almacen:${alm.codigo}`}
              transform={`translate(${ax}, ${ay})`}
              className="cursor-pointer"
              onClick={(e) => {
                onSelectAlmacen?.(alm.codigo);
                clicIcono(e, { tipo: 'almacen', codigo: alm.codigo });
              }}
            >
              <title>{tooltip}</title>
              {esSel && (
                <circle r={TAMANO_ICONO * 1.15} fill="none" stroke="#60A5FA" strokeWidth="0.18" className="animate-pulse" />
              )}
              <g transform={`scale(${ESCALA_BODEGA})`} filter="url(#sombraIcono)">
                <Bodega color={color} letra={letra} />
              </g>

              {/* Indicador visual de semáforo de ocupación del almacén */}
              {!isCentral && (
                <circle
                  cx={TAMANO_ICONO * 0.55}
                  cy={-TAMANO_ICONO * 0.55}
                  r={0.25}
                  fill={semAlmColor}
                  stroke="#FFFFFF"
                  strokeWidth="0.08"
                />
              )}

              {/* Etiqueta de stock debajo del almacén */}
              <text
                y={TAMANO_ICONO * 0.8 + 0.35}
                textAnchor="middle"
                fontSize="0.45"
                fontWeight="bold"
                fill="#1F2937"
                stroke="white"
                strokeWidth="0.15"
                paintOrder="stroke"
                className="select-none"
              >
                {isCentral ? '∞' : `${stockAlm}u (${pctAlm}%)`}
              </text>
            </g>
          );
        })}

        {/* Vehículos (estilo GPS): vistos desde arriba, giran para que su frente apunte hacia donde avanzan.
            Con ruta, la animación los hace recorrer su camino calle por calle (ver useLayoutEffect). */}
        {(() => {
          // Vehículos detenidos en el mismo punto se separan un poco en círculo para no taparse
          const grupos = new Map<string, UnidadTransporte[]>();
          visibles.filter((u) => pasaFiltroUnidad(u, filtros)).forEach((u) => {
            const k = `${u.ubicacionActual.posX},${u.ubicacionActual.posY}`;
            grupos.set(k, [...(grupos.get(k) ?? []), u]);
          });

          const elementos: React.ReactNode[] = [];
          grupos.forEach((grupo, clave) => {
            const [gx, gy] = clave.split(',').map(Number);
            const detenidos = grupo.filter((u) => !trazados.has(u.codigo));

            grupo.forEach((u) => {
              const iDet = detenidos.indexOf(u);
              const desp = detenidos.length > 1 && iDet >= 0 ? TAMANO_ICONO : 0;
              const a = iDet >= 0 ? (2 * Math.PI * iDet) / Math.max(1, detenidos.length) - Math.PI / 2 : 0;
              const ux = toSvgX(gx) + desp * Math.cos(a);
              const uy = toSvgY(gy) + desp * Math.sin(a);

              const averiado = u.estadoOperativo === 'AVERIADA';
              const isSelected =
                (seleccion?.tipo === 'vehiculo' && seleccion.codigo === u.codigo) ||
                selectedUnidadCodigo === u.codigo;

              const capU = u.capacidadMaxima || 1;
              const pctU = Math.min(100, Math.round((u.cargaActual / capU) * 100));
              const semU = nivelSemaforoUT(u);
              const semUColor =
                semU === 'ROJO'
                  ? '#EF4444'
                  : semU === 'AMBAR'
                  ? '#F59E0B'
                  : semU === 'VERDE'
                  ? '#22C55E'
                  : '#9CA3AF';

              // "En movimiento" si el backend lo marca en ruta, si tiene una ruta pendiente o si se movió
              const prev = memoriaMov.current.get(u.codigo);
              const seMovio = !!prev && (prev.x !== u.ubicacionActual.posX || prev.y !== u.ubicacionActual.posY);
              const enMovimiento = u.estadoOperativo === 'EN_RUTA' || rutaDe.has(u.codigo) || seMovio;
              // Cada vehículo conserva el color de su tipo; detenido o averiado se ve atenuado
              const color = COLOR_TIPO[u.tipoNombre] ?? COLOR_TIPO.Bicicleta;

              // Ángulo para los que no tienen ruta: según su último movimiento o el último conocido
              const angNuevo =
                (prev && anguloDe(u.ubicacionActual.posX - prev.x, u.ubicacionActual.posY - prev.y)) ??
                anguloPorRuta(u, rutaDe.get(u.codigo)) ??
                prev?.ang ??
                0;
              const ang = prev ? giroMasCorto(prev.ang, angNuevo) : angNuevo;
              memoriaMov.current.set(u.codigo, { x: u.ubicacionActual.posX, y: u.ubicacionActual.posY, ang });

              // Con ruta: posición y giro los pone la animación. Sin ruta: se ubica directo.
              const sobreRuta = trazados.has(u.codigo) && !averiado;
              const tooltipU = `${u.tipoNombre} ${u.codigo} · Carga: ${u.cargaActual}/${capU} (${pctU}%) · Semáforo: ${semU}${
                averiado ? ' · averiado' : enMovimiento ? ' · en ruta' : ' · detenido'
              }`;

              elementos.push(
                // 1) Grupo exterior: se desplaza
                <g
                  key={`vehiculo-${u.codigo}`}
                  ref={(el) => {
                    if (el) nodoVehiculo.current.set(u.codigo, el);
                    else nodoVehiculo.current.delete(u.codigo);
                  }}
                  data-sel={`vehiculo:${u.codigo}`}
                  onClick={(e) => {
                    onSelectUnidad(u.codigo);
                    clicIcono(e, { tipo: 'vehiculo', codigo: u.codigo });
                  }}
                  className="cursor-pointer"
                  transform={sobreRuta ? undefined : `translate(${ux} ${uy})`}
                >
                  <title>{tooltipU}</title>
                  {isSelected && <circle r={TAMANO_ICONO * 0.85} fill="none" stroke="#60A5FA" strokeWidth="0.16" className="animate-pulse" />}

                  {/* Badge semáforo de ocupación de la UT */}
                  <circle
                    cx={-TAMANO_ICONO * 0.45}
                    cy={-TAMANO_ICONO * 0.45}
                    r={0.2}
                    fill={semUColor}
                    stroke="#FFFFFF"
                    strokeWidth="0.06"
                  />

                  {/* 2) Grupo interior: gira sobre el centro del vehículo (el eje Y del SVG va hacia abajo,
                      por eso el signo negativo). Sobre ruta, el giro lo pone la animación según la calle */}
                  <g
                    ref={(el) => {
                      if (el) giroVehiculo.current.set(u.codigo, el);
                      else giroVehiculo.current.delete(u.codigo);
                    }}
                    transform={sobreRuta ? undefined : `rotate(${averiado ? 0 : -ang})`}
                  >
                    <g
                      transform={`scale(${ESCALA_VEHICULO})`}
                      filter="url(#sombraIcono)"
                      opacity={averiado ? 0.55 : !enMovimiento ? 0.75 : 1}
                    >
                      <VehiculoRealista tipo={u.tipoNombre} color={color} />
                    </g>
                  </g>

                  {/* Averiado: triángulo de advertencia con el color de riesgo y el número de su tipo
                      (1 leve · 2 moderada · 3 grave). Late para llamar la atención. No gira. */}
                  {averiado && (() => {
                    const t = tipoAveriaDe(u);
                    const info = AVERIA_TIPO[t];
                    return (
                      <g transform={`translate(${TAMANO_ICONO * 0.5}, ${-TAMANO_ICONO * 0.55})`}>
                        <title>{`Avería ${info.nombre}: ${info.detalle}`}</title>
                        <polygon points="0,-0.78 0.72,0.5 -0.72,0.5" fill="none" stroke={info.color} strokeWidth="0.12" className="animate-pulse" />
                        <polygon points="0,-0.55 0.5,0.36 -0.5,0.36" fill={info.color} stroke="#111827" strokeWidth="0.07" strokeLinejoin="round" />
                        <text y="0.27" textAnchor="middle" fontSize="0.42" fontWeight="bold" fill={info.texto} className="select-none">
                          {t}
                        </text>
                      </g>
                    );
                  })()}

                  {/* 3) Código del vehículo y ocupación: se desplaza con él pero no gira, así siempre se lee derecho */}
                  <text
                    y={TAMANO_ICONO * 0.75 + 0.35}
                    textAnchor="middle"
                    fontSize="0.42"
                    fontWeight="bold"
                    fill="#1F2937"
                    stroke="white"
                    strokeWidth="0.15"
                    paintOrder="stroke"
                    className="select-none"
                  >
                    {`${u.codigo} (${pctU}%)`}
                  </text>
                </g>,
              );
            });
          });
          return elementos;
        })()}
      </svg>

      {/* Popup de detalle: pequeño, junto al ícono y siguiéndolo */}
      {contenidoPopup && (
        <div
          ref={popupRef}
          role="dialog"
          aria-label={contenidoPopup.titulo}
          className="absolute left-0 top-0 z-20 w-56 rounded-xl border border-gray-200 bg-white shadow-lg text-xs"
          style={{ visibility: 'hidden' }}
          onClick={(e) => e.stopPropagation()}
        >
          <div className="flex items-center gap-2 px-3 py-2 border-b border-gray-100">
            <span className="w-2.5 h-2.5 rounded-full shrink-0" style={{ backgroundColor: contenidoPopup.color }} />
            <span className="font-bold text-gray-900 truncate flex-1">{contenidoPopup.titulo}</span>
            {contenidoPopup.insignia}
            <button type="button" onClick={() => setSeleccion(null)} aria-label="Cerrar" className="text-gray-400 hover:text-gray-700 shrink-0">
              <X size={14} />
            </button>
          </div>
          <div className="px-3 py-2.5 space-y-2">{contenidoPopup.cuerpo}</div>
          <div className="px-3 pb-2 text-[10px] text-gray-400">Clic en el mapa vacío o Esc para quitar la selección</div>
        </div>
      )}

      {/* Indicadores globales de ocupación (G01–G04): esquina superior izquierda del mapa */}
      {ocupacionGlobal && (
        <div className="absolute top-3 left-3 z-10 w-48 rounded-lg border border-gray-200 bg-white/95 shadow-sm px-3 py-2 select-none">
          <div className="text-[11px] font-semibold uppercase tracking-wide text-gray-500 mb-1.5">Ocupación global</div>
          {([
            ['Flota', ocupacionGlobal.flota],
            ['Almacenes', ocupacionGlobal.almacenes],
          ] as const).map(([etiqueta, ind]) => (
            <div key={etiqueta} className="flex items-center gap-2 py-0.5" title={ind.detalle}>
              <span className="w-2 h-2 rounded-full shrink-0" style={{ backgroundColor: ind.color }} />
              <span className="w-20 text-xs text-gray-700 truncate">{etiqueta}</span>
              <div className="flex-1 h-1.5 rounded-full bg-gray-100 overflow-hidden">
                <div
                  className="h-1.5 rounded-full transition-[width] duration-500"
                  style={{ width: `${Math.min(100, Math.max(0, ind.pct))}%`, backgroundColor: ind.color }}
                />
              </div>
              <span className="w-9 text-right text-xs font-bold text-gray-900">{ind.pct}%</span>
            </div>
          ))}
        </div>
      )}

      {/* Rosa de los vientos (Norte), a la derecha para que siga visible al abrir el panel */}
      <div className="absolute top-3 right-4 flex flex-col items-center select-none pointer-events-none opacity-80">
        <span className="text-xs font-black text-gray-800 leading-none mb-0.5">N</span>
        <svg className="w-3 h-5 fill-gray-800" viewBox="0 0 24 36"><polygon points="12,0 20,20 12,14 4,20" /></svg>
      </div>

      {/* Controles de zoom: acercar, alejar y volver a la vista inicial */}
      <div className="absolute top-12 right-3 z-10 flex flex-col rounded-lg border border-gray-300 bg-white/90 shadow-sm overflow-hidden">
        <button type="button" onClick={() => zoomAnimado(vista.zoom * 1.5)} title="Acercar" className="p-1.5 hover:bg-white text-gray-700">
          <Plus className="w-4 h-4" />
        </button>
        <button type="button" onClick={() => zoomAnimado(vista.zoom / 1.5)} title="Alejar" className="p-1.5 hover:bg-white text-gray-700 border-t border-gray-200">
          <Minus className="w-4 h-4" />
        </button>
        <button type="button" onClick={vistaInicial} title="Vista inicial" className="p-1.5 hover:bg-white text-gray-700 border-t border-gray-200">
          <Maximize2 className="w-4 h-4" />
        </button>
      </div>

      {/* Botón de leyenda: en la esquina inferior derecha, flotando sobre el mapa (no lo mueve) */}
      <button
        onClick={() => setShowLeyenda(!showLeyenda)}
        title={showLeyenda ? 'Ocultar leyenda' : 'Ver leyenda'}
        aria-expanded={showLeyenda}
        className={`absolute bottom-3 right-3 z-10 flex items-center gap-1.5 h-8 px-3 rounded-lg border shadow-sm text-xs font-semibold transition-colors ${
          showLeyenda
            ? 'bg-(--color-brand-500) text-white border-(--color-brand-500)'
            : 'bg-white/90 hover:bg-white text-gray-700 border-gray-300'
        }`}
      >
        <Info className="w-3.5 h-3.5" />
        Leyenda
      </button>

      {/* La leyenda se abre justo encima del botón */}
      {showLeyenda && (
        // Ocupa como máximo el alto del mapa (de arriba hasta el botón): si no entra, la leyenda hace scroll
        // por dentro en vez de cortarse arriba. El espacio vacío deja pasar los clics al mapa.
        <div className="absolute top-3 bottom-13 right-3 z-20 flex flex-col justify-end pointer-events-none">
          <div className="pointer-events-auto min-h-0 flex">
            <LeyendaMapa onClose={() => setShowLeyenda(false)} escenario={escenario} />
          </div>
        </div>
      )}
    </div>
  );
};