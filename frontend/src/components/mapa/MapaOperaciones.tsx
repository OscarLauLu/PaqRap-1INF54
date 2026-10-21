import React, { useLayoutEffect, useMemo, useRef, useState } from 'react';
import { Almacen, BloqueoVial, Pedido, Ubicacion, UnidadTransporte } from '../../types';
import { Info, Wrench } from 'lucide-react';
import { LeyendaMapa } from './LeyendaMapa';
import { Bodega, PinPedido, VehiculoRealista } from './IconosMapa';

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
}

interface MapaOperacionesProps {
  almacenes: Almacen[];
  unidades: UnidadTransporte[];
  pedidos: Pedido[];
  bloqueos?: BloqueoVial[];
  rutas?: RutaMapa[];
  selectedUnidadCodigo: string | null;
  onSelectUnidad: (codigo: string) => void;
  /** Nombre del escenario para el título de la leyenda (opcional) */
  escenario?: string;
  /** Si la simulación está en pausa, los vehículos se congelan donde están */
  pausado?: boolean;
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
const ESCALA = 1.2;
const ANCHO_VISTA = BASE.w / ESCALA;
const ALTO_VISTA = BASE.h / ESCALA;


// ===== Orientación de los vehículos (estilo GPS) =====
// ===== Paleta del mapa: colores vibrantes y distintos para cada elemento =====
// Pedidos (semáforo): verde, ámbar y rojo · Vehículos: azul, morado y rosa
// Almacenes: naranja, cian e índigo · Avería: amarillo con negro (como señal de peligro)
const COLOR_TIPO: Record<string, string> = { Auto: '#2563EB', Moto: '#9333EA', Bicicleta: '#EC4899' };
const COLOR_ALMACEN = { central: '#EA580C', norte: '#0891B2', este: '#4F46E5' };
const COLOR_AVERIA = '#FACC15';
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

const limitar = (v: number, min: number, max: number) => Math.min(max, Math.max(min, v));

/** Mantiene la vista dentro de la ciudad */
const ajustarCentro = (x: number, y: number) => ({
  x: limitar(x, BASE.x + ANCHO_VISTA / 2, BASE.x + BASE.w - ANCHO_VISTA / 2),
  y: limitar(y, BASE.y + ALTO_VISTA / 2, BASE.y + BASE.h - ALTO_VISTA / 2),
});

export const MapaOperaciones: React.FC<MapaOperacionesProps> = ({
  almacenes,
  unidades,
  pedidos,
  bloqueos = [],
  rutas = [],
  selectedUnidadCodigo,
  onSelectUnidad,
  escenario,
  pausado = false,
}) => {
  const [showLeyenda, setShowLeyenda] = useState(false);

  // Última posición y ángulo de cada vehículo: con eso se calcula hacia dónde se movió
  // y se mantiene su orientación cuando se detiene (como un auto estacionado).
  const memoriaMov = useRef(new Map<string, { x: number; y: number; ang: number }>());
  // Última posición vista de TODOS los vehículos (también los ocultos) y cuándo se movieron por última vez.
  // Sirve para mostrar en el mapa a los que se están moviendo aunque el backend no les marque ruta.
  const ultimoMovimiento = useRef(new Map<string, { x: number; y: number; t: number }>());

  // --- Animación sobre la ruta (se mueve el DOM directamente, sin volver a dibujar con React) ---
  const nodoVehiculo = useRef(new Map<string, SVGGElement>());   // grupo que se desplaza
  const giroVehiculo = useRef(new Map<string, SVGGElement>());   // grupo que gira
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

  // --- Desplazamiento arrastrando, como en Google Maps (sin zoom) ---
  const [centro, setCentro] = useState({ x: BASE.x + BASE.w / 2, y: BASE.y + BASE.h / 2 });
  const svgRef = useRef<SVGSVGElement>(null);
  const arrastre = useRef<{ px: number; py: number; cx: number; cy: number; movido: boolean } | null>(null);
  const [arrastrando, setArrastrando] = useState(false);

  // Unidades del mapa (km) por píxel de pantalla. Con preserveAspectRatio "slice" el mapa llena
  // todo el recuadro (sin franjas vacías), así que manda el lado con más píxeles por km.
  const kmPorPixel = () => {
    const el = svgRef.current;
    if (!el) return 0;
    const r = el.getBoundingClientRect();
    return Math.min(ANCHO_VISTA / r.width, ALTO_VISTA / r.height);
  };

  const alPresionar = (e: React.PointerEvent<SVGSVGElement>) => {
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
      setArrastrando(true);
      e.currentTarget.setPointerCapture(e.pointerId);
    }
    const k = kmPorPixel();
    setCentro(ajustarCentro(a.cx - dx * k, a.cy - dy * k));
  };

  const alSoltar = () => {
    arrastre.current = null;
    setArrastrando(false);
  };

  const viewBox = `${centro.x - ANCHO_VISTA / 2} ${centro.y - ALTO_VISTA / 2} ${ANCHO_VISTA} ${ALTO_VISTA}`;

  // En el mapa se dibujan los pedidos ya asignados a un vehículo: planificados, en ruta, o que
  // figuran como parada pendiente de alguna ruta. Así toda línea de ruta termina en un punto.
  // (El panel derecho sigue contando todos los pedidos.)
  const codigosEnRuta = new Set(rutas.flatMap((r) => r.codigosPedidos ?? []));
  const pedidosVisibles = pedidos.filter(
    (p) => p.destino && (p.estado === 'PLANIFICADO' || p.estado === 'EN_RUTA' || codigosEnRuta.has(p.codigo)),
  );

  // La matriz tiene 70 de ancho por 50 de alto: 1 unidad SVG = 1 km.
  // El origen del backend está abajo a la izquierda y el de SVG arriba a la izquierda:
  const toSvgX = (x: number) => x;
  const toSvgY = (y: number) => 50 - y;

  // Cada vez que llegan posiciones nuevas, cada vehículo con ruta recorre su camino desde donde
  // está dibujado hasta su nueva posición, en el mismo tiempo que tarda la siguiente consulta.
  useLayoutEffect(() => {
    const ahora = performance.now();
    unidades.forEach((u) => {
      const t = trazados.get(u.codigo);
      if (!t || u.estadoOperativo === 'AVERIADO') {
        animaciones.current.delete(u.codigo);
        recorrido.current.delete(u.codigo);
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
  }, [unidades, trazados, pausado]);

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
      u.estadoOperativo === 'AVERIADO' ||
      ahoraMs - t < VENTANA_MOVIMIENTO_MS ||
      u.codigo === selectedUnidadCodigo
    ) {
      conTrabajo.add(u.codigo);
    }
  });
  const visibles = SOLO_VEHICULOS_CON_TRABAJO ? unidades.filter((u) => conTrabajo.has(u.codigo)) : unidades;

  return (
    <div className="relative w-full h-full min-h-[480px] bg-[#f8fafc] rounded-xl border border-gray-200 overflow-hidden">
      <svg
        ref={svgRef}
        className={`w-full h-full bg-[#f8fafc] touch-none ${arrastrando ? 'cursor-grabbing' : 'cursor-grab'}`}
        viewBox={viewBox}
        preserveAspectRatio="xMidYMid slice"
        onPointerDown={alPresionar}
        onPointerMove={alMover}
        onPointerUp={alSoltar}
        onPointerLeave={alSoltar}
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

        {/* Rutas planificadas: línea gris punteada */}
        {rutas.map((r) => {
          if (r.puntos.length < 2) return null;
          let d: string;
          if (r.esCamino) {
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
              d={d}
              fill="none"
              stroke="#6B7280"
              strokeWidth="0.12"
              strokeDasharray="0.45 0.3"
              strokeLinecap="round"
              strokeLinejoin="round"
              className="pointer-events-none"
            />
          );
        })}

        {/* Bloqueos de calle activos: tramo en negro con una barrera al medio */}
        {bloqueos
          .filter((b) => b.activo)
          .map((b) => {
            const pts = parsePuntos(b.coordenadasNodos);
            if (pts.length < 2) return null;
            const linea = pts.map(([x, y]) => `${toSvgX(x)},${toSvgY(y)}`).join(' ');
            const medio = pts[Math.floor(pts.length / 2)] as [number, number];
            const mx = toSvgX(medio[0]);
            const my = toSvgY(medio[1]);
            return (
              <g key={`bloqueo-${b.id}`} className="select-none">
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
          return (
            <g key={`pedido-${p.id}`} className="cursor-pointer">
              <title>{`Pedido ${p.codigo}`}</title>
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
                {p.codigo}
              </text>
            </g>
          );
        })}

        {/* Almacenes: bodega con su letra */}
        {almacenes.map((alm) => {
          const ax = toSvgX(alm.ubicacion.posX);
          const ay = toSvgY(alm.ubicacion.posY);
          const esNoroeste = /n[-\s]?o/i.test(alm.nombre) || alm.nombre.toLowerCase().includes('norte');
          const letra = alm.tipo === 'CENTRAL' ? 'C' : esNoroeste ? 'N' : 'E';
          const color = alm.tipo === 'CENTRAL' ? COLOR_ALMACEN.central : esNoroeste ? COLOR_ALMACEN.norte : COLOR_ALMACEN.este;

          return (
            <g key={`alm-${alm.codigo}`} transform={`translate(${ax}, ${ay})`} className="cursor-default">
              <title>{alm.nombre}</title>
              <g transform={`scale(${ESCALA_BODEGA})`} filter="url(#sombraIcono)">
                <Bodega color={color} letra={letra} />
              </g>
            </g>
          );
        })}

        {/* Vehículos (estilo GPS): vistos desde arriba, giran para que su frente apunte hacia donde avanzan.
            Con ruta, la animación los hace recorrer su camino calle por calle (ver useLayoutEffect). */}
        {(() => {
          // Vehículos detenidos en el mismo punto se separan un poco en círculo para no taparse
          const grupos = new Map<string, UnidadTransporte[]>();
          visibles.forEach((u) => {
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

              const averiado = u.estadoOperativo === 'AVERIADO';
              const isSelected = u.codigo === selectedUnidadCodigo;

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

              elementos.push(
                // 1) Grupo exterior: se desplaza
                <g
                  key={`vehiculo-${u.codigo}`}
                  ref={(el) => {
                    if (el) nodoVehiculo.current.set(u.codigo, el);
                    else nodoVehiculo.current.delete(u.codigo);
                  }}
                  onClick={() => onSelectUnidad(u.codigo)}
                  className="cursor-pointer"
                  transform={sobreRuta ? undefined : `translate(${ux} ${uy})`}
                >
                  <title>{`${u.tipoNombre} ${u.codigo}${averiado ? ' · averiado' : enMovimiento ? ' · en ruta' : ' · detenido'}`}</title>
                  {isSelected && <circle r={TAMANO_ICONO * 0.8} fill="none" stroke="#60A5FA" strokeWidth="0.15" className="animate-pulse" />}

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

                  {/* Averiado (cualquier tipo de vehículo): llave negra en un círculo amarillo, como señal de peligro (no gira) */}
                  {averiado && (
                    <g transform={`translate(${TAMANO_ICONO * 0.45}, ${-TAMANO_ICONO * 0.45})`}>
                      <circle r="0.32" fill={COLOR_AVERIA} stroke="#111827" strokeWidth="0.07" />
                      <Wrench width={0.38} height={0.38} x={-0.19} y={-0.19} stroke="#111827" strokeWidth={2.8} />
                    </g>
                  )}

                  {/* 3) Código del vehículo: se desplaza con él pero no gira, así siempre se lee derecho */}
                  <text
                    y={TAMANO_ICONO * 0.75 + 0.35}
                    textAnchor="middle"
                    fontSize="0.45"
                    fontWeight="bold"
                    fill="#1F2937"
                    stroke="white"
                    strokeWidth="0.15"
                    paintOrder="stroke"
                    className="select-none"
                  >
                    {u.codigo}
                  </text>
                </g>,
              );
            });
          });
          return elementos;
        })()}
      </svg>

      {/* Rosa de los vientos (Norte), a la derecha para que siga visible al abrir el panel */}
      <div className="absolute top-3 right-4 flex flex-col items-center select-none pointer-events-none opacity-80">
        <span className="text-xs font-black text-gray-800 leading-none mb-0.5">N</span>
        <svg className="w-3 h-5 fill-gray-800" viewBox="0 0 24 36"><polygon points="12,0 20,20 12,14 4,20" /></svg>
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
        <div className="absolute bottom-13 right-3 z-20">
          <LeyendaMapa onClose={() => setShowLeyenda(false)} escenario={escenario} />
        </div>
      )}
    </div>
  );
};