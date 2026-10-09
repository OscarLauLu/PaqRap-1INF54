import React, { useRef, useState } from 'react';
import { Almacen, BloqueoVial, Pedido, Ubicacion, UnidadTransporte } from '../../types';
import { Car, Bike, Info, Wrench } from 'lucide-react';
import { LeyendaMapa } from './LeyendaMapa';

/** Recorrido planificado de un vehículo: sus paradas en orden (empezando por su origen). */
export interface RutaMapa {
  unidadCodigo: string;
  puntos: Ubicacion[];
  /** Códigos de los pedidos que aún faltan entregar en esta ruta */
  codigosPedidos?: string[];
  /** Indica que "puntos" ya es el camino calle por calle, así que se dibuja sin calcular nada */
  esCamino?: boolean;
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

// Tamaño de almacenes y vehículos respecto al diseño original (0.55 ≈ un poco más de la mitad),
// para que vayan a la par con los puntos de clientes más pequeños.
const ESCALA_MARCADORES = 0.55;

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
}) => {
  const [showLeyenda, setShowLeyenda] = useState(false);

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

        {/* Bloqueos de calle activos: tramo en negro con un rectángulo rojo al medio */}
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
                <rect x={mx - 0.45} y={my - 0.28} width="0.9" height="0.56" rx="0.12" fill="#DC2626" />
                <text x={mx + 0.65} y={my - 0.35} fontSize="0.5" fontWeight="bold" fill="#111827">Bloqueo</text>
              </g>
            );
          })}

        {/* Marcadores de Clientes / Pedidos asignados, con un punto pequeño */}
        {pedidosVisibles.map((p) => {
          const px = toSvgX(p.destino.posX);
          const py = toSvgY(p.destino.posY);
          return (
            <g key={`pedido-${p.id}`} className="cursor-pointer group">
              <circle cx={px} cy={py} r={0.3} fill="#2563EB" />
              <text x={px} y={py - 0.55} textAnchor="middle" fontSize="0.45" fontWeight="bold" fill="#2563EB" className="select-none">
                {p.codigo}
              </text>
            </g>
          );
        })}

        {/* Almacenes */}
        {almacenes.map((alm) => {
          const ax = toSvgX(alm.ubicacion.posX);
          const ay = toSvgY(alm.ubicacion.posY);
          const esNoroeste = /n[-\s]?o/i.test(alm.nombre) || alm.nombre.toLowerCase().includes('norte');
          const letra = alm.tipo === 'CENTRAL' ? 'C' : esNoroeste ? 'N' : 'E';
          const color = alm.tipo === 'CENTRAL' ? '#DC2626' : esNoroeste ? '#F97316' : '#9333EA';

          return (
            <g key={`alm-${alm.codigo}`} transform={`translate(${ax}, ${ay}) scale(${ESCALA_MARCADORES})`} className="cursor-pointer">
              <rect x="-1.5" y="-1.5" width="3" height="3" rx="0.5" fill={color} />
              <path d="M -1 -0.2 L 0 -1.2 L 1 -0.2" stroke="white" strokeWidth="0.2" fill="none" />
              <text x="0" y="1" textAnchor="middle" fill="white" fontSize="1.5" fontWeight="bold">{letra}</text>
            </g>
          );
        })}

        {/* Unidades de Transporte */}
        {unidades.map((u) => {
          const ux = toSvgX(u.ubicacionActual.posX);
          const uy = toSvgY(u.ubicacionActual.posY);
          const isSelected = u.codigo === selectedUnidadCodigo;
          const bgColor = u.tipoNombre === 'Auto' ? '#3B82F6' : u.tipoNombre === 'Moto' ? '#16A34A' : '#EAB308';

          return (
            <g key={`vehiculo-${u.codigo}`} transform={`translate(${ux}, ${uy}) scale(${ESCALA_MARCADORES})`} onClick={() => onSelectUnidad(u.codigo)} className="cursor-pointer group">
              {isSelected && <circle r="2.5" fill="none" stroke="#60A5FA" strokeWidth="0.3" className="animate-pulse" />}
              {u.estadoOperativo === 'AVERIADO' ? (
                // Avería: círculo oscuro con una llave, igual que en la leyenda
                <>
                  <circle r="1.35" fill="#374151" />
                  <Wrench width={1.6} height={1.6} x="-0.8" y="-0.8" stroke="white" strokeWidth={2.2} />
                </>
              ) : u.tipoNombre === 'Auto' ? (
                <Car width={2.5} height={2.5} x="-1.25" y="-1.25" stroke={bgColor} strokeWidth={2} />
              ) : (
                <Bike width={2.5} height={2.5} x="-1.25" y="-1.25" stroke={bgColor} strokeWidth={2} />
              )}
              <text x="0" y="2" textAnchor="middle" fontSize="1" fontWeight="bold" fill="#1F2937">{u.codigo}</text>
            </g>
          );
        })}
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