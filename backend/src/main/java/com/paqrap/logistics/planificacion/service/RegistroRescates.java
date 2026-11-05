package com.paqrap.logistics.planificacion.service;

import com.paqrap.logistics.planificacion.model.Ruta;
import com.paqrap.logistics.planificacion.model.TramoRescate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Operación diaria: tramos de rescate (trasvase) de las rutas guardadas. La entidad Ruta no los
 * persiste (serían la tabla "trasvase" del documento 62), así que se guardan aquí, por id de ruta,
 * y se vuelven a adjuntar cada vez que se cargan las rutas desde la base.
 * Con la base H2 en memoria, un reinicio borra ambas cosas a la vez.
 */
@Component
public class RegistroRescates {

    private final Map<Long, TramoRescate> porRuta = new ConcurrentHashMap<>();

    public void registrar(Long idRuta, TramoRescate tramo) {
        if (idRuta != null && tramo != null) porRuta.put(idRuta, tramo);
    }

    /** true si la avería ya tiene una ruta de rescate despachada. */
    public boolean tieneRescate(String codigoAveria) {
        return codigoAveria != null && porRuta.values().stream().anyMatch(t -> codigoAveria.equals(t.getCodigoAveria()));
    }

    /** Vuelve a poner el tramo de rescate en las rutas recién cargadas de la base. */
    public void adjuntar(Collection<Ruta> rutas) {
        if (porRuta.isEmpty()) return;
        for (Ruta r : rutas) {
            if (r != null && r.getId() != null && r.getRescate() == null) {
                r.setRescate(porRuta.get(r.getId()));
            }
        }
    }
}
