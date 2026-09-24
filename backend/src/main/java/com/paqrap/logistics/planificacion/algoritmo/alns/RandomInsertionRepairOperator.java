package com.paqrap.logistics.planificacion.algoritmo.alns;

import com.paqrap.logistics.almacen.model.Almacen;
import com.paqrap.logistics.almacen.model.AlmacenCentral;
import com.paqrap.logistics.flota.model.EstadoOperativo;
import com.paqrap.logistics.flota.model.UnidadTransporte;
import com.paqrap.logistics.pedidos.model.Pedido;
import com.paqrap.logistics.planificacion.model.ParadaRuta;
import com.paqrap.logistics.planificacion.model.Ruta;
import com.paqrap.logistics.redvial.model.RedVial;
import com.paqrap.logistics.redvial.model.Ubicacion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Operador de Reparación por Inserción Aleatoria.
 * Inserta los pedidos liberados en posiciones factibles escogidas al azar,
 * proporcionando diversificación frente a los operadores greedy y regret.
 */
public class RandomInsertionRepairOperator extends RepairOperator {

    private final RedVial redVial;
    private final List<Almacen> almacenes;
    private final List<UnidadTransporte> flota;
    private final Random random;

    public RandomInsertionRepairOperator(RedVial redVial, List<Almacen> almacenes,
                                         List<UnidadTransporte> flota, Random random) {
        this.redVial = redVial;
        this.almacenes = almacenes != null ? almacenes : new ArrayList<>();
        this.flota = flota != null ? flota : new ArrayList<>();
        this.random = random != null ? random : new Random();
    }

    @Override
    public String getNombre() {
        return "RandomInsertionRepairOperator";
    }

    @Override
    public void reparar(PlanSolution solucion, List<Pedido> liberados) {
        if (solucion == null || liberados == null || liberados.isEmpty()) return;

        LocalDateTime ahora = obtenerTiempoReferencia(solucion, liberados);
        List<Pedido> pendientes = new ArrayList<>(liberados);
        Collections.shuffle(pendientes, random);

        for (Pedido p : pendientes) {
            boolean insertado = intentarInsertarAleatoriamente(solucion, p, ahora);
            if (!insertado) {
                insertado = intentarCrearNuevaRuta(solucion, p, ahora);
            }
            if (!insertado) {
                solucion.getPedidosNoAsignados().add(p);
            } else {
                solucion.getPedidosNoAsignados().remove(p);
            }
        }

        solucion.recalcularMetricas(redVial, ahora);
    }

    private boolean intentarInsertarAleatoriamente(PlanSolution solucion, Pedido p, LocalDateTime ahora) {
        // Recolectar todas las inserciones factibles
        List<int[]> posicionesFactibles = new ArrayList<>();

        for (int ri = 0; ri < solucion.getRutas().size(); ri++) {
            Ruta r = solucion.getRutas().get(ri);
            UnidadTransporte unidad = r.getUnidadTransporte();
            if (unidad == null || !unidad.isActivo() || unidad.getEstadoOperativo() == EstadoOperativo.AVERIADA) {
                continue;
            }

            int capacidadMax = (unidad.getTipo() != null) ? unidad.getTipo().getCapacidadMaxima() : 24;
            int cargaActual = r.getParadas().stream()
                    .mapToInt(parada -> parada.getPedido() != null ? parada.getPedido().getCantidadUnidades() : 0)
                    .sum();

            if (cargaActual + p.getCantidadUnidades() > capacidadMax) continue;

            for (int pos = 0; pos <= r.getParadas().size(); pos++) {
                if (validaPlazosConInsercion(r, p, pos, ahora)) {
                    posicionesFactibles.add(new int[]{ri, pos});
                }
            }
        }

        if (posicionesFactibles.isEmpty()) return false;

        // Seleccionar una posición factible al azar
        int[] seleccion = posicionesFactibles.get(random.nextInt(posicionesFactibles.size()));
        Ruta ruta = solucion.getRutas().get(seleccion[0]);
        ParadaRuta nuevaParada = ParadaRuta.builder()
                .pedido(p)
                .tiempoServicioMin(60)
                .build();
        ruta.getParadas().add(seleccion[1], nuevaParada);
        return true;
    }

    private boolean validaPlazosConInsercion(Ruta r, Pedido p, int pos, LocalDateTime ahora) {
        UnidadTransporte unidad = r.getUnidadTransporte();
        double velocidad = (unidad != null && unidad.getTipo() != null && unidad.getTipo().getVelocidadPromedioKmH() > 0)
                ? unidad.getTipo().getVelocidadPromedioKmH() : 40.0;

        Ubicacion actual = obtenerOrigenRuta(r);
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
                return false;
            }

            reloj = llegada.plusMinutes(parada.getTiempoServicioMin());
            actual = dest;
        }
        return true;
    }

    private boolean intentarCrearNuevaRuta(PlanSolution solucion, Pedido p, LocalDateTime ahora) {
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

    private Ubicacion obtenerOrigenRuta(Ruta r) {
        if (r.getAlmacenOrigen() != null && r.getAlmacenOrigen().getUbicacion() != null) {
            return r.getAlmacenOrigen().getUbicacion();
        }
        if (r.getUnidadTransporte() != null && r.getUnidadTransporte().getUbicacionActual() != null) {
            return r.getUnidadTransporte().getUbicacionActual();
        }
        return new Ubicacion(27, 14);
    }

    private LocalDateTime obtenerTiempoReferencia(PlanSolution solucion, List<Pedido> liberados) {
        if (!solucion.getRutas().isEmpty() && solucion.getRutas().get(0).getFechaHoraGeneracion() != null) {
            return solucion.getRutas().get(0).getFechaHoraGeneracion();
        }
        return liberados.stream()
                .map(Pedido::getFechaHoraRegistro)
                .filter(java.util.Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
    }
}
