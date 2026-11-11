package com.paqrap.logistics.planificacion.dto;

import com.paqrap.logistics.planificacion.model.EstadoRuta;
import com.paqrap.logistics.redvial.model.Ubicacion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RutaDTO {
    private Long id;
    private String codigo;
    private String unidadCodigo;
    private String tipoVehiculo;
    private String almacenOrigenCodigo;
    private String almacenOrigenNombre;
    private LocalDateTime fechaHoraGeneracion;
    private double distanciaTotalKm;
    private int tiempoEstimadoMin;
    private double costoTotal;
    private EstadoRuta estado;
    private List<ParadaRutaDTO> paradas;

    // Esquinas por las que pasa la ruta, en orden. El frontend las necesita para dibujar
    // el recorrido real en el mapa, ya que las paradas solas no dicen por qué calles se va.
    private List<Ubicacion> camino;

    // Regreso al terminar las entregas: almacén con stock más cercano y hora en que el vehículo llega.
    // El camino ya incluye el tramo de regreso, así el mapa muestra al vehículo volviendo.
    private String almacenRetornoCodigo;
    private LocalDateTime horaLlegadaAlmacen;

    // Solo rutas de rescate: la unidad va al punto de la avería, trasvasa 30 min y luego entrega.
    // El camino empieza en "partida" y pasa por "puntoTrasvase" antes de la primera parada.
    private String rescataA;
    private Ubicacion partida;
    private Ubicacion puntoTrasvase;
    private LocalDateTime llegadaTrasvase;
    private LocalDateTime finTrasvase;

    /** Copia los datos del tramo de rescate (si la ruta lo tiene) y devuelve este mismo DTO. */
    public RutaDTO conRescate(com.paqrap.logistics.planificacion.model.TramoRescate t) {
        if (t != null) {
            this.rescataA = t.getUnidadAveriada();
            this.partida = t.getPartida();
            this.puntoTrasvase = t.getPuntoTrasvase();
            this.llegadaTrasvase = t.getLlegada();
            this.finTrasvase = t.getFin();
        }
        return this;
    }
}
