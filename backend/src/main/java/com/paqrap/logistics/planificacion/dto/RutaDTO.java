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
}
