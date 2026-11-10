package com.paqrap.logistics.planificacion.model;

import com.paqrap.logistics.redvial.model.Nodo;
import com.paqrap.logistics.redvial.model.Ubicacion;
import com.paqrap.logistics.redvial.service.RedVialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Camino calle por calle que dibuja el mapa para una ruta: almacén de salida → cada parada → almacén
 * de regreso, uniendo cada par de puntos con la ruta mínima de la red vial (esquiva bloqueos vigentes).
 * Lo usan la operación diaria (PlanificacionService) y las sesiones de simulación.
 */
public final class CaminoRuta {

    private static final Logger log = LoggerFactory.getLogger(CaminoRuta.class);

    private CaminoRuta() {
    }

    /**
     * Puntos de visita de la ruta en orden. Si se indica {@code retorno}, se agrega al final para que el
     * mapa muestre el regreso del vehículo al almacén.
     */
    public static List<Ubicacion> puntosDeVisita(Ruta r, Ubicacion retorno) {
        List<Ubicacion> puntos = new ArrayList<>();
        if (r.getRescate() != null) {
            // Ruta de rescate: desde donde estaba la unidad hasta el punto de la avería (trasvase)
            puntos.add(r.getRescate().getPartida());
            puntos.add(r.getRescate().getPuntoTrasvase());
        } else {
            puntos.add(r.getAlmacenOrigen().getUbicacion());
        }
        r.getParadas().stream()
                .sorted(Comparator.comparingInt(ParadaRuta::getOrden))
                .filter(p -> p.getPedido() != null && p.getPedido().getDestino() != null)
                .forEach(p -> puntos.add(p.getPedido().getDestino()));
        if (retorno != null) {
            puntos.add(retorno);
        }
        return puntos;
    }

    /**
     * Une los puntos con el camino mínimo libre de bloqueos vigentes en {@code instante}.
     * <p>
     * El camino dibujado nunca atraviesa un bloqueo: si entre dos puntos los bloqueos no dejan pasar,
     * el camino se corta ahí (el vehículo queda a la espera de que se libere la calle) en lugar de
     * dibujar una línea recta que cruza el bloqueo, como hacía antes la ruta de contingencia.
     */
    public static List<Ubicacion> unir(List<Ubicacion> puntos, RedVialService redVialService, LocalDateTime instante) {
        List<Ubicacion> camino = new ArrayList<>();
        if (puntos.isEmpty()) return camino;
        camino.add(puntos.get(0));
        for (int i = 1; i < puntos.size(); i++) {
            Ubicacion desde = puntos.get(i - 1);
            Ubicacion hasta = puntos.get(i);
            if (desde.getPosX() == hasta.getPosX() && desde.getPosY() == hasta.getPosY()) {
                continue; // mismo punto: no hay tramo que dibujar
            }
            List<Nodo> nodos = redVialService.caminoLibre(
                    desde.getPosX(), desde.getPosY(), hasta.getPosX(), hasta.getPosY(), instante);
            if (nodos == null || nodos.size() < 2) {
                // Sin camino libre: se corta aquí para no dibujar el cruce por un bloqueo
                log.warn("Sin camino libre de bloqueos entre ({},{}) y ({},{}) en {}: el camino dibujado se corta en ese punto.",
                        desde.getPosX(), desde.getPosY(), hasta.getPosX(), hasta.getPosY(), instante);
                break;
            }
            // El primer nodo es "desde", que ya está en el camino
            for (int k = 1; k < nodos.size(); k++) {
                camino.add(nodos.get(k).aUbicacion());
            }
        }
        return camino;
    }
}
