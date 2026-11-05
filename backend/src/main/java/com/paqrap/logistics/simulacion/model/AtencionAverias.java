package com.paqrap.logistics.simulacion.model;

import com.paqrap.logistics.flota.model.Averia;
import com.paqrap.logistics.flota.model.EstadoOperativo;
import com.paqrap.logistics.flota.model.UnidadTransporte;
import com.paqrap.logistics.pedidos.model.EstadoPedido;
import com.paqrap.logistics.planificacion.model.EstadoRuta;
import com.paqrap.logistics.planificacion.model.ParadaRuta;
import com.paqrap.logistics.planificacion.model.Ruta;
import com.paqrap.logistics.redvial.model.Ubicacion;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ciclo de vida de una avería (FAQ 3), común a la operación diaria y a las sesiones de simulación:
 * <ul>
 *   <li>Al ocurrir: la unidad queda AVERIADA e inmóvil en el punto de la falla, con su carga.
 *       La avería NO bloquea la vía.</li>
 *   <li>Tipo 1: 2 h en el lugar; luego retoma su ruta con el horario corrido esas 2 h.</li>
 *   <li>Tipo 2 y 3: 4 h en el lugar; luego se traslada de forma instantánea al almacén central con la
 *       carga que no se trasvasó, y esos pedidos vuelven a planificarse. Se reincorpora al final del
 *       turno siguiente (tipo 2) o a los 2 días como mínimo en el turno 15:00-23:00 (tipo 3).</li>
 * </ul>
 * Solo cambia objetos de dominio; quien la llama se encarga de persistirlos.
 */
public final class AtencionAverias {

    public enum Cambio { NINGUNO, TRASLADADA_AL_CENTRAL, REINCORPORADA }

    private AtencionAverias() {
    }

    /** La unidad queda AVERIADA en el punto de la falla y se calcula su reincorporación. */
    public static void iniciar(Averia a, UnidadTransporte u) {
        a.setUnidad(u);
        if (a.getHoraReincorporacion() == null) {
            a.setHoraReincorporacion(a.calcularReincorporacion(null));
        }
        if (a.getUbicacionFalla() != null) {
            u.setUbicacionActual(new Ubicacion(a.getUbicacionFalla().getPosX(), a.getUbicacionFalla().getPosY()));
        }
        u.setEstadoOperativo(EstadoOperativo.AVERIADA);
    }

    /**
     * Avanza una avería activa hasta el instante dado.
     *
     * @param rutaEnCurso      ruta sin completar de la unidad (puede ser null)
     * @param central          ubicación del almacén central
     * @param paradasLiberadas aquí se agregan las paradas que se quitan de la ruta al trasladar la unidad
     *                         (la operación diaria las borra de la base)
     */
    public static Cambio avanzar(Averia a, UnidadTransporte u, Ruta rutaEnCurso, LocalDateTime instante,
                                 Ubicacion central, List<ParadaRuta> paradasLiberadas) {
        if (a.isResuelta() || instante == null || a.getFechaHoraEvento() == null) return Cambio.NINGUNO;
        Cambio cambio = Cambio.NINGUNO;

        LocalDateTime finEnElLugar = a.getFechaHoraEvento().plusHours(a.horasEnElLugar());
        if (a.seTrasladaAlCentral() && !instante.isBefore(finEnElLugar) && !mismaUbicacion(u.getUbicacionActual(), central)) {
            u.setUbicacionActual(new Ubicacion(central.getPosX(), central.getPosY()));
            if (rutaEnCurso != null) {
                liberarPendientes(rutaEnCurso, paradasLiberadas);
            }
            cambio = Cambio.TRASLADADA_AL_CENTRAL;
        }

        if (a.getHoraReincorporacion() != null && !instante.isBefore(a.getHoraReincorporacion())) {
            if (!a.seTrasladaAlCentral() && rutaEnCurso != null && rutaEnCurso.getEstado() != EstadoRuta.COMPLETADA) {
                correrHorario(rutaEnCurso, Duration.between(a.getFechaHoraEvento(), a.getHoraReincorporacion()));
                u.setEstadoOperativo(EstadoOperativo.EN_RUTA);
            } else {
                u.setEstadoOperativo(EstadoOperativo.DISPONIBLE);
            }
            a.setResuelta(true);
            cambio = Cambio.REINCORPORADA;
        }
        return cambio;
    }

    /**
     * La carga no entregada deja la ruta: sus pedidos vuelven a REGISTRADO para que el planificador los
     * asigne a otra unidad, y la ruta se da por terminada.
     */
    static void liberarPendientes(Ruta ruta, List<ParadaRuta> paradasLiberadas) {
        Iterator<ParadaRuta> it = ruta.getParadas().iterator();
        while (it.hasNext()) {
            ParadaRuta p = it.next();
            if (p.isEntregada()) continue;
            if (p.getPedido() != null) {
                p.getPedido().setEstado(EstadoPedido.REGISTRADO);
                p.getPedido().setRutaAsignadaId(null);
                p.getPedido().setUnidadAsignadaId(null);
            }
            it.remove();
            p.setRuta(null);
            if (paradasLiberadas != null) paradasLiberadas.add(p);
        }
        ruta.setEstado(EstadoRuta.COMPLETADA);
    }

    /**
     * Tras una avería tipo 1 la unidad retoma su ruta: lo que faltaba se corre el tiempo que estuvo
     * detenida. Si ya regresaba al almacén, se corre todo el horario para que el regreso también se demore.
     */
    static void correrHorario(Ruta ruta, Duration demora) {
        boolean habiaPendientes = ruta.getParadas().stream().anyMatch(p -> !p.isEntregada());
        for (ParadaRuta p : ruta.getParadas()) {
            if (p.getHoraEstimadaLlegada() != null && (!habiaPendientes || !p.isEntregada())) {
                p.setHoraEstimadaLlegada(p.getHoraEstimadaLlegada().plus(demora));
            }
        }
        if (!habiaPendientes && ruta.getFechaHoraGeneracion() != null) {
            ruta.setFechaHoraGeneracion(ruta.getFechaHoraGeneracion().plus(demora));
        }
    }

    /** Resumen plano de una avería para las respuestas de la API (la entidad tiene referencias cíclicas). */
    public static Map<String, Object> resumen(Averia a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("codigo", a.getCodigo());
        m.put("idUnidad", a.getUnidad() != null ? a.getUnidad().getCodigo() : null);
        m.put("tipo", a.getTipo() != null ? a.getTipo().name() : null);
        m.put("ubicacion", a.getUbicacionFalla());
        m.put("fechaHoraEvento", a.getFechaHoraEvento());
        m.put("horaReincorporacion", a.getHoraReincorporacion());
        m.put("resuelta", a.isResuelta());
        return m;
    }

    private static boolean mismaUbicacion(Ubicacion a, Ubicacion b) {
        return a != null && b != null && a.getPosX() == b.getPosX() && a.getPosY() == b.getPosY();
    }
}
