package com.paqrap.logistics.planificacion.model;

import com.paqrap.logistics.almacen.model.Almacen;
import com.paqrap.logistics.flota.model.Averia;
import com.paqrap.logistics.flota.model.EstadoOperativo;
import com.paqrap.logistics.flota.model.UnidadTransporte;
import com.paqrap.logistics.pedidos.model.EstadoPedido;
import com.paqrap.logistics.redvial.model.Ubicacion;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Rescate de la carga de una unidad averiada mediante un trasvase (diagrama de clases: Trasvase,
 * 30 min en el punto de la avería). Decisión del equipo: el ACO solo arma rutas que salen de un
 * almacén, así que el rescate se arma con esta regla simple, aparte del ACO:
 * <ol>
 *   <li>Candidatas: unidades libres (DISPONIBLE) o que regresan al almacén sin carga pendiente.</li>
 *   <li>Deben llegar y terminar el trasvase antes de que la averiada deje el lugar (tipo 2 y 3: a las
 *       4 h) o se reincorpore (tipo 1: a las 2 h).</li>
 *   <li>Tipo 1 solo se rescata si esperar las 2 h haría llegar tarde algún pedido.</li>
 *   <li>Gana la que llega primero; se lleva los pedidos que le caben, por orden de plazo. Lo que no cabe
 *       se queda en la averiada (tipo 2 y 3: viaja con ella al central y vuelve a planificarse).</li>
 *   <li>Desde el punto de la avería entrega yendo siempre a la parada más cercana.</li>
 * </ol>
 */
public final class PlanificadorTrasvase {

    private PlanificadorTrasvase() {
    }

    /**
     * Arma la ruta de rescate para la avería, o devuelve null si hoy no hace falta o no hay quién llegue a
     * tiempo. Si la arma, mueve las paradas rescatadas desde la ruta averiada a la nueva ruta.
     *
     * @param rutaAveriada  ruta en curso de la unidad averiada
     * @param enCurso       ruta en curso de cada unidad (para saber quién está regresando)
     * @param codigoRuta    código para la nueva ruta
     */
    public static Ruta planificar(Averia a, Ruta rutaAveriada, Collection<UnidadTransporte> flota,
                                  Map<String, Ruta> enCurso, LocalDateTime instante,
                                  Collection<? extends Almacen> almacenes, String codigoRuta) {
        if (a == null || a.isResuelta() || a.getUnidad() == null || rutaAveriada == null || instante == null) return null;
        UnidadTransporte averiada = a.getUnidad();
        Ubicacion punto = a.getUbicacionFalla() != null ? a.getUbicacionFalla() : averiada.getUbicacionActual();
        if (punto == null) return null;

        List<ParadaRuta> pendientes = new ArrayList<>(rutaAveriada.getParadas().stream()
                .filter(p -> !p.isEntregada() && p.getPedido() != null)
                .sorted(Comparator.comparing(p -> p.getPedido().getPlazoLimiteEntrega(),
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList());
        if (pendientes.isEmpty()) return null;

        LocalDateTime limite = a.seTrasladaAlCentral()
                ? a.getFechaHoraEvento().plusHours(a.horasEnElLugar())
                : a.getHoraReincorporacion();
        if (limite == null || !limite.isAfter(instante)) return null;
        if (!a.seTrasladaAlCentral() && !esperarHariaLlegarTarde(pendientes, a)) return null;

        // Candidatas, de la que llega primero a la que llega último
        record Candidata(UnidadTransporte u, Ruta regreso, LocalDateTime llegada, LocalDateTime fin, int km) {}
        List<Candidata> candidatas = new ArrayList<>();
        for (UnidadTransporte u : flota) {
            if (u == averiada || u.isDadaDeBaja() || u.getTipo() == null || u.getUbicacionActual() == null) continue;
            Ruta regreso = null;
            if (u.getEstadoOperativo() == EstadoOperativo.EN_RUTA) {
                regreso = enCurso.get(u.getCodigo());
                if (regreso == null || regreso.getRescate() != null || regreso.cargaPendiente() > 0) continue;
            } else if (u.getEstadoOperativo() != EstadoOperativo.DISPONIBLE) {
                continue;
            }
            int km = distancia(u.getUbicacionActual(), punto);
            LocalDateTime llegada = instante.plusSeconds(segundos(km, u));
            LocalDateTime fin = llegada.plusMinutes(TramoRescate.DURACION_TRASVASE_MIN);
            if (fin.isAfter(limite)) continue;
            candidatas.add(new Candidata(u, regreso, llegada, fin, km));
        }
        candidatas.sort(Comparator.comparing(Candidata::llegada)
                .thenComparing(c -> -c.u().getTipo().getCapacidadMaxima())
                .thenComparing(c -> c.u().getCodigo()));

        for (Candidata c : candidatas) {
            List<ParadaRuta> rescatadas = new ArrayList<>();
            int carga = 0;
            for (ParadaRuta p : pendientes) {
                int q = p.getPedido().getCantidadUnidades();
                if (carga + q <= c.u().getTipo().getCapacidadMaxima()) {
                    rescatadas.add(p);
                    carga += q;
                }
            }
            if (rescatadas.isEmpty()) continue;   // no le cabe ni un pedido: se prueba la siguiente
            return armarRuta(a, averiada, rutaAveriada, c.u(), c.regreso(), punto, instante, c.llegada(), c.fin(),
                    c.km(), rescatadas, carga, almacenes, codigoRuta);
        }
        return null;
    }

    /** Tipo 1: con la demora de 2 h, ¿algún pedido pendiente llegaría después de su plazo? */
    private static boolean esperarHariaLlegarTarde(List<ParadaRuta> pendientes, Averia a) {
        Duration demora = Duration.between(a.getFechaHoraEvento(), a.getHoraReincorporacion());
        return pendientes.stream().anyMatch(p -> p.getHoraEstimadaLlegada() != null
                && p.getPedido().getPlazoLimiteEntrega() != null
                && p.getHoraEstimadaLlegada().plus(demora).isAfter(p.getPedido().getPlazoLimiteEntrega()));
    }

    private static Ruta armarRuta(Averia a, UnidadTransporte averiada, Ruta rutaAveriada, UnidadTransporte rescata,
                                  Ruta regreso, Ubicacion punto, LocalDateTime salida, LocalDateTime llegada,
                                  LocalDateTime fin, int kmIda, List<ParadaRuta> rescatadas, int carga,
                                  Collection<? extends Almacen> almacenes, String codigoRuta) {
        Ruta ruta = Ruta.builder()
                .codigo(codigoRuta)
                .unidadTransporte(rescata)
                .almacenOrigen(almacenMasCercano(rescata.getUbicacionActual(), almacenes))
                .fechaHoraGeneracion(salida)
                .estado(EstadoRuta.EN_EJECUCION)
                .rescate(TramoRescate.builder()
                        .codigoAveria(a.getCodigo())
                        .unidadAveriada(averiada.getCodigo())
                        .partida(new Ubicacion(rescata.getUbicacionActual().getPosX(), rescata.getUbicacionActual().getPosY()))
                        .puntoTrasvase(new Ubicacion(punto.getPosX(), punto.getPosY()))
                        .salida(salida)
                        .llegada(llegada)
                        .fin(fin)
                        .cantidadProductos(carga)
                        .build())
                .build();

        // Entregas desde el punto de la avería: siempre a la parada pendiente más cercana
        List<ParadaRuta> porVisitar = new ArrayList<>(rescatadas);
        Ubicacion actual = punto;
        LocalDateTime t = fin;
        int km = kmIda;
        int orden = 1;
        while (!porVisitar.isEmpty()) {
            final Ubicacion desde = actual;
            ParadaRuta siguiente = porVisitar.stream()
                    .min(Comparator.comparingInt((ParadaRuta p) -> distancia(desde, p.getPedido().getDestino()))
                            .thenComparing(p -> p.getPedido().getCodigo()))
                    .orElseThrow();
            porVisitar.remove(siguiente);
            int tramo = distancia(actual, siguiente.getPedido().getDestino());
            km += tramo;
            t = t.plusSeconds(segundos(tramo, rescata));
            rutaAveriada.getParadas().remove(siguiente);
            siguiente.setRuta(ruta);
            siguiente.setOrden(orden++);
            siguiente.setEntregada(false);
            siguiente.setHoraEstimadaLlegada(t);
            ruta.getParadas().add(siguiente);
            siguiente.getPedido().setEstado(EstadoPedido.PLANIFICADO);
            siguiente.getPedido().setUnidadAsignadaId(rescata.getCodigo());
            t = t.plusMinutes(siguiente.getTiempoServicioMin());
            actual = siguiente.getPedido().getDestino();
        }
        ruta.setDistanciaTotalKm(km);
        ruta.setCostoTotal(km * rescata.getTipo().getCostoPorKm());
        ruta.setTiempoEstimadoMin((int) Duration.between(salida, t).toMinutes());

        // La unidad que regresaba se desvía: su regreso termina aquí
        if (regreso != null) regreso.setEstado(EstadoRuta.COMPLETADA);
        rescata.setEstadoOperativo(EstadoOperativo.EN_RUTA);
        return ruta;
    }

    private static Almacen almacenMasCercano(Ubicacion desde, Collection<? extends Almacen> almacenes) {
        return almacenes.stream()
                .filter(x -> x.getUbicacion() != null)
                .min(Comparator.comparingInt((Almacen x) -> distancia(desde, x.getUbicacion()))
                        .thenComparing(Almacen::getCodigo))
                .orElse(null);
    }

    static int distancia(Ubicacion a, Ubicacion b) {
        return Math.abs(a.getPosX() - b.getPosX()) + Math.abs(a.getPosY() - b.getPosY());
    }

    private static long segundos(int km, UnidadTransporte u) {
        double v = u.getTipo().getVelocidadPromedioKmH() > 0 ? u.getTipo().getVelocidadPromedioKmH() : 25.0;
        return Math.round(km / v * 3600);
    }
}
