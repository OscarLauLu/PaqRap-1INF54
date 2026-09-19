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
import java.util.UUID;

/**
 * Operador de Reparación Regret-k (Ropke & Pisinger, 2006).
 * Prioriza la inserción de pedidos cuya diferencia entre la k-ésima mejor
 * y la mejor posición de inserción es mayor (mayor "arrepentimiento").
 * Esto evita que pedidos difíciles de insertar queden sin asignar.
 */
public class RegretInsertionRepairOperator extends RepairOperator {

    private final RedVial redVial;
    private final List<Almacen> almacenes;
    private final List<UnidadTransporte> flota;
    private final int k;

    public RegretInsertionRepairOperator(RedVial redVial, List<Almacen> almacenes,
                                         List<UnidadTransporte> flota, int k) {
        this.redVial = redVial;
        this.almacenes = almacenes != null ? almacenes : new ArrayList<>();
        this.flota = flota != null ? flota : new ArrayList<>();
        this.k = Math.max(2, k);
    }

    @Override
    public String getNombre() {
        return "RegretInsertionRepairOperator(k=" + k + ")";
    }

    @Override
    public void reparar(PlanSolution solucion, List<Pedido> liberados) {
        if (solucion == null || liberados == null || liberados.isEmpty()) return;

        LocalDateTime ahora = obtenerTiempoReferencia(solucion, liberados);
        List<Pedido> pendientes = new ArrayList<>(liberados);

        while (!pendientes.isEmpty()) {
            Pedido mejorPedido = null;
            int mejorRutaIdx = -1;
            int mejorPosicion = -1;
            double maxRegret = -Double.MAX_VALUE;
            double menorCostoGlobal = Double.MAX_VALUE;

            for (Pedido p : pendientes) {
                List<double[]> inserciones = calcularInserciones(solucion, p, ahora);

                if (inserciones.isEmpty()) continue;

                Collections.sort(inserciones, (a, b) -> Double.compare(a[0], b[0]));

                double mejorCosto = inserciones.get(0)[0];
                double regret;
                if (inserciones.size() >= k) {
                    regret = inserciones.get(k - 1)[0] - mejorCosto;
                } else {
                    // Si hay menos de k opciones, usar la peor disponible + penalización
                    regret = inserciones.get(inserciones.size() - 1)[0] - mejorCosto + 1000.0;
                }

                if (regret > maxRegret || (regret == maxRegret && mejorCosto < menorCostoGlobal)) {
                    maxRegret = regret;
                    menorCostoGlobal = mejorCosto;
                    mejorPedido = p;
                    mejorRutaIdx = (int) inserciones.get(0)[1];
                    mejorPosicion = (int) inserciones.get(0)[2];
                }
            }

            if (mejorPedido == null) {
                // No se pudo insertar ningún pedido pendiente
                for (Pedido p : pendientes) {
                    if (!intentarCrearNuevaRuta(solucion, p, ahora)) {
                        solucion.getPedidosNoAsignados().add(p);
                    }
                }
                break;
            }

            // Insertar el pedido con mayor regret en su mejor posición
            if (mejorRutaIdx >= 0 && mejorRutaIdx < solucion.getRutas().size()) {
                Ruta ruta = solucion.getRutas().get(mejorRutaIdx);
                ParadaRuta nuevaParada = ParadaRuta.builder()
                        .pedido(mejorPedido)
                        .tiempoServicioMin(60)
                        .build();
                ruta.getParadas().add(mejorPosicion, nuevaParada);
            } else {
                if (!intentarCrearNuevaRuta(solucion, mejorPedido, ahora)) {
                    solucion.getPedidosNoAsignados().add(mejorPedido);
                }
            }

            pendientes.remove(mejorPedido);
            solucion.getPedidosNoAsignados().remove(mejorPedido);
        }

        solucion.recalcularMetricas(redVial, ahora);
    }

    /**
     * Calcula todas las inserciones factibles para un pedido.
     * Retorna lista de {costoMarginal, rutaIdx, posición}.
     */
    private List<double[]> calcularInserciones(PlanSolution solucion, Pedido p, LocalDateTime ahora) {
        List<double[]> resultado = new ArrayList<>();

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

            double tarifa = (unidad.getTipo() != null) ? unidad.getTipo().getCostoPorKm() : 8.0;

            for (int pos = 0; pos <= r.getParadas().size(); pos++) {
                double incremento = calcularIncrementoDistancia(r, p, pos, ahora);
                if (incremento == Double.MAX_VALUE) continue;

                if (validaPlazosConInsercion(r, p, pos, ahora)) {
                    resultado.add(new double[]{incremento * tarifa, ri, pos});
                }
            }
        }

        return resultado;
    }

    private double calcularIncrementoDistancia(Ruta r, Pedido p, int pos, LocalDateTime ahora) {
        Ubicacion origen = obtenerOrigenRuta(r);
        Ubicacion nodoAnt = (pos == 0) ? origen
                : r.getParadas().get(pos - 1).getPedido().getDestino();
        Ubicacion nodoSig = (pos < r.getParadas().size())
                ? r.getParadas().get(pos).getPedido().getDestino() : null;
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
                             (r.getUnidadTransporte().getId() != null && r.getUnidadTransporte().getId().equals(u.getId()))));

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
