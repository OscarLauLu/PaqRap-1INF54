package com.paqrap.logistics.almacen.dto;

import com.paqrap.logistics.redvial.model.Ubicacion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlmacenDTO {
    private String codigo;
    private String nombre;
    private String tipo;
    private Ubicacion ubicacion;
    // null = stock infinito (Almacén Central); ver Almacen.stockActual (docs/62, CHECK de "almacen")
    private Integer stockActual;
    private Integer capacidadMaxima;
    private double porcentajeOcupacion;
    private boolean superaAlertaOcupacion;
}
