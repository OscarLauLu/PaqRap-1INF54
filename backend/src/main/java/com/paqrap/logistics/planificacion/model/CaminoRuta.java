package com.paqrap.logistics.planificacion.model;

import com.paqrap.logistics.redvial.model.Nodo;
import com.paqrap.logistics.redvial.model.Tramo;
import com.paqrap.logistics.redvial.model.Ubicacion;
import com.paqrap.logistics.redvial.service.RedVialService;

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

    private CaminoRuta() {
    }

    /**
     * Puntos de visita de la ruta en orden. Si se indica {@code retorno}, se agrega al final para que el
     * mapa muestre el regreso del vehículo al almacén.
     */
    public static List<Ubicacion> puntosDeVisita(Ruta r, Ubicacion retorno) {
        List<Ubicacion> puntos = new ArrayList<>();
        puntos.add(r.getAlmacenOrigen().getUbicacion());
        r.getParadas().stream()
                .sorted(Comparator.comparingInt(ParadaRuta::getOrden))
                .filter(p -> p.getPedido() != null && p.getPedido().getDestino() != null)
                .forEach(p -> puntos.add(p.getPedido().getDestino()));
        if (retorno != null) {
            puntos.add(retorno);
        }
        return puntos;
    }

    /** Une los puntos con la ruta mínima; si entre dos puntos no hay camino se unen directo. */
    public static List<Ubicacion> unir(List<Ubicacion> puntos, RedVialService redVialService, LocalDateTime instante) {
        List<Ubicacion> camino = new ArrayList<>();
        if (puntos.isEmpty()) return camino;
        camino.add(puntos.get(0));
        for (int i = 1; i < puntos.size(); i++) {
            Ubicacion desde = puntos.get(i - 1);
            Ubicacion hasta = puntos.get(i);
            List<Tramo> tramos = redVialService.calcularRutaMinima(
                    desde.getPosX(), desde.getPosY(), hasta.getPosX(), hasta.getPosY(), instante);
            if (tramos == null || tramos.isEmpty()) {
                camino.add(hasta);
                continue;
            }
            // Los tramos son de doble sentido y pueden venir invertidos: se toma el extremo distinto al actual
            Nodo actual = Nodo.deUbicacion(camino.get(camino.size() - 1));
            for (Tramo t : tramos) {
                Nodo siguiente = t.getNodoOrigen().equals(actual) ? t.getNodoDestino() : t.getNodoOrigen();
                camino.add(siguiente.aUbicacion());
                actual = siguiente;
            }
        }
        return camino;
    }
}
