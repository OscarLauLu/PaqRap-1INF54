package com.paqrap.logistics.flota.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Representa un periodo de mantenimiento preventivo para una unidad (FAQ 19).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mantenimiento {
    private String codigoVehiculo;
    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;
    
    public boolean estaActivoEn(LocalDateTime instante) {
        if (instante == null || fechaHoraInicio == null || fechaHoraFin == null) return false;
        return !instante.isBefore(fechaHoraInicio) && !instante.isAfter(fechaHoraFin);
    }
}
