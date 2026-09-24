package com.paqrap.logistics.planificacion.algoritmo.alns;

import com.paqrap.logistics.almacen.model.Almacen;
import com.paqrap.logistics.almacen.model.AlmacenCentral;
import com.paqrap.logistics.flota.model.EstadoOperativo;
import com.paqrap.logistics.flota.model.UnidadTransporte;
import com.paqrap.logistics.pedidos.model.Pedido;
import com.paqrap.logistics.planificacion.model.ParadaRuta;
import com.paqrap.logistics.planificacion.model.Ruta;
import com.paqrap.logistics.redvial.model.Nodo;
import com.paqrap.logistics.redvial.model.RedVial;
import com.paqrap.logistics.redvial.model.Ubicacion;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Operador de Reparación Voraz por Holgura (RF-04, RF-07, RF-08).
 * Ordena los pedidos liberados por menor holgura (mayor urgencia) y los reinserta
 * en la posición de menor costo marginal que cumpla estrictamente capacidades y plazos.
 */
public class HolguraGreedyRepairOperator extends RepairOperator {

    private final RedVial redVial;
    private final List<Almacen> almacenes;
    private final List<UnidadTransporte> flota;

    public HolguraGreedyRepairOperator(RedVial redVial, List<Almacen> almacenes, List<UnidadTransporte> flota) {
        this.redVial = redVial;
        this.almacenes = almacenes != null ? almacenes : new ArrayList<>();
        this.flota = flota != null ? flota : new ArrayList<>();
    }

    @Override
    public String getNombre() {
        return "HolguraGreedyRepairOperator";
    }

    @Override
    public void reparar(PlanSolution solucion, List<Pedido> liberados) {
        if (solucion == null || liberados == null || liberados.isEmpty()) {
            return;
        }

        LocalDateTime ahora = (solucion != null && !solucion.getRutas().isEmpty() && solucion.getRutas().get(0).getFechaHoraGeneracion() != null)
                ? solucion.getRutas().get(0).getFechaHoraGeneracion()
                : (liberados.stream().map(Pedido::getFechaHoraRegistro).filter(java.util.Objects::nonNull).min(LocalDateTime::compareTo).orElse(LocalDateTime.now()));

        // 1. Ordenar pedidos por menor holgura (RF-04: pedidos más urgentes primero)
        List<Pedido> ordenados = new ArrayList<>(liberados);
        ordenados.sort(Comparator.comparing(p -> p.calcularHolgura(ahora)));

        // 2. Reinsertar cada pedido vorazmente en la mejor posición de menor incremento de costo
        for (Pedido p : ordenados) {
            boolean insertado = intentarInsertarEnMejorPosicion(solucion, p, ahora);

            // Si no cabe en ninguna ruta activa, intentar abrir una nueva ruta con unidad libre
            if (!insertado) {
                insertado = intentarCrearNuevaRutaParaPedido(solucion, p, ahora);
            }

            if (!insertado) {
                solucion.getPedidosNoAsignados().add(p);
            } else {
                solucion.getPedidosNoAsignados().remove(p);
            }
        }

        // 3. Recalcular métricas de todas las rutas afectadas
        solucion.recalcularMetricas(redVial, ahora);
    }

    private boolean intentarInsertarEnMejorPosicion(PlanSolution solucion, Pedido p, LocalDateTime ahora) {
        Ruta mejorRuta = null;
        int mejorPosicion = -1;
        double menorCostoMarginal = Double.MAX_VALUE;

        for (Ruta r : solucion.getRutas()) {
            UnidadTransporte unidad = r.getUnidadTransporte();
            if (unidad == null || !unidad.isActivo() || unidad.getEstadoOperativo() == EstadoOperativo.AVERIADA) {
                continue;
            }

            int capacidadMax = (unidad.getTipo() != null) ? unidad.getTipo().getCapacidadMaxima() : 24;
            int cargaActual = r.getParadas().stream()
                    .mapToInt(parada -> parada.getPedido() != null ? parada.getPedido().getCantidadUnidades() : 0)
                    .sum();

            if (cargaActual + p.getCantidadUnidades() > capacidadMax) {
                continue; // Supera capacidad (RF-07)
            }

            double tarifa = (unidad.getTipo() != null) ? unidad.getTipo().getCostoPorKm() : 8.0;

            // Probar insertar en cada posición posible de la ruta (0 .. size)
            for (int pos = 0; pos <= r.getParadas().size(); pos++) {
                double incrementoDist = calcularIncrementoDistancia(r, p, pos, ahora);
                if (incrementoDist == Double.MAX_VALUE) continue;

                double costoMarginal = incrementoDist * tarifa;
                if (costoMarginal < menorCostoMarginal) {
                    // Validar si la inserción en 'pos' sigue cumpliendo los plazos
                    if (validaPlazosConInsercion(r, p, pos, ahora)) {
                        menorCostoMarginal = costoMarginal;
                        mejorRuta = r;
                        mejorPosicion = pos;
                    }
                }
            }
        }

        if (mejorRuta != null && mejorPosicion >= 0) {
            ParadaRuta nuevaParada = ParadaRuta.builder()
                    .pedido(p)
                    .tiempoServicioMin(60)
                    .build();
            mejorRuta.getParadas().add(mejorPosicion, nuevaParada);
            return true;
        }

        return false;
    }

    private boolean intentarCrearNuevaRutaParaPedido(PlanSolution solucion, Pedido p, LocalDateTime ahora) {
        // Buscar un vehículo libre que no tenga ruta activa asignada
        for (UnidadTransporte u : flota) {
            if (!u.isActivo() || u.getEstadoOperativo() == EstadoOperativo.AVERIADA) continue;
            boolean yaTieneRuta = solucion.getRutas().stream()
                    .anyMatch(r -> r.getUnidadTransporte() != null &&
                            (java.util.Objects.equals(r.getUnidadTransporte().getCodigo(), u.getCodigo()) ||
                             (r.getUnidadTransporte().getCodigo() != null && r.getUnidadTransporte().getCodigo().equals(u.getCodigo()))));

            if (!yaTieneRuta) {
                int cap = (u.getTipo() != null) ? u.getTipo().getCapacidadMaxima() : 24;
                if (p.getCantidadUnidades() <= cap) {
                    Almacen almacenCentral = almacenes.stream()
                            .filter(a -> a instanceof AlmacenCentral)
                            .findFirst()
                            .orElse(!almacenes.isEmpty() ? almacenes.get(0) : null);

                    Ruta nuevaRuta = Ruta.builder()
                            .codigo("RUT-" + UUID.randomUUID().toString().substring(0, 8))
                            .unidadTransporte(u)
                            .almacenOrigen(almacenCentral)
                            .fechaHoraGeneracion(ahora)
                            .distanciaTotalKm(0.0)
                            .costoTotal(0.0)
                            .tiempoEstimadoMin(0)
                            .paradas(new ArrayList<>())
                            .build();

                    ParadaRuta parada = ParadaRuta.builder()
                            .pedido(p)
                            .tiempoServicioMin(60)
                            .build();
                    nuevaRuta.agregarParada(parada);
                    solucion.getRutas().add(nuevaRuta);
                    return true;
                }
            }
        }
        return false;
    }

    private double calcularIncrementoDistancia(Ruta r, Pedido p, int pos, LocalDateTime ahora) {
        Ubicacion origen = (r.getAlmacenOrigen() != null && r.getAlmacenOrigen().getUbicacion() != null)
                ? r.getAlmacenOrigen().getUbicacion()
                : (r.getUnidadTransporte() != null && r.getUnidadTransporte().getUbicacionActual() != null
                ? r.getUnidadTransporte().getUbicacionActual() : new Ubicacion(27, 14));

        Ubicacion nodoAnt = (pos == 0)
                ? origen
                : r.getParadas().get(pos - 1).getPedido().getDestino();

        Ubicacion nodoSig = (pos < r.getParadas().size())
                ? r.getParadas().get(pos).getPedido().getDestino()
                : null;

        Ubicacion destP = p.getDestino() != null ? p.getDestino() : new Ubicacion(27, 14);

        double d1 = redVial.distanciaMinima(nodoAnt, destP, ahora);
        if (d1 == Double.MAX_VALUE) return Double.MAX_VALUE;

        if (nodoSig == null) {
            return d1;
        } else {
            double d2 = redVial.distanciaMinima(destP, nodoSig, ahora);
            if (d2 == Double.MAX_VALUE) return Double.MAX_VALUE;
            double dOriginal = redVial.distanciaMinima(nodoAnt, nodoSig, ahora);
            if (dOriginal == Double.MAX_VALUE) dOriginal = nodoAnt.distanciaOrtogonalA(nodoSig);
            return (d1 + d2) - dOriginal;
        }
    }

    private boolean validaPlazosConInsercion(Ruta r, Pedido p, int pos, LocalDateTime ahora) {
        UnidadTransporte unidad = r.getUnidadTransporte();
        double velocidad = (unidad != null && unidad.getTipo() != null && unidad.getTipo().getVelocidadPromedioKmH() > 0)
                ? unidad.getTipo().getVelocidadPromedioKmH() : 40.0;

        Ubicacion actual = (r.getAlmacenOrigen() != null && r.getAlmacenOrigen().getUbicacion() != null)
                ? r.getAlmacenOrigen().getUbicacion()
                : (unidad != null && unidad.getUbicacionActual() != null ? unidad.getUbicacionActual() : new Ubicacion(27, 14));

        LocalDateTime reloj = ahora;
        List<ParadaRuta> copiaParadas = new ArrayList<>(r.getParadas());
        copiaParadas.add(pos, ParadaRuta.builder().pedido(p).tiempoServicioMin(60).build());

        for (ParadaRuta parada : copiaParadas) {
            Pedido pedido = parada.getPedido();
            if (pedido == null) continue;
            Ubicacion dest = pedido.getDestino() != null ? pedido.getDestino() : new Ubicacion(27, 14);
            double dist = redVial.distanciaMinima(actual, dest, reloj);
            if (dist == Double.MAX_VALUE) return false;

            long minViaje = (long) Math.ceil((dist / velocidad) * 60.0);
            LocalDateTime llegada = reloj.plusMinutes(minViaje);

            if (pedido.getPlazoLimiteEntrega() != null && llegada.isAfter(pedido.getPlazoLimiteEntrega())) {
                return false; // Violación de plazo (RF-08)
            }

            reloj = llegada.plusMinutes(parada.getTiempoServicioMin());
            actual = dest;
        }
        return true;
    }
}
