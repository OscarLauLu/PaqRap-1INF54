package com.paqrap.logistics.redvial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para el registro manual de un bloqueo vial individual, en contraste con la carga
 * masiva por archivo mensual (RF-10, RF-11). Se usa desde la pantalla de "Registro manual
 * de bloqueos" del frontend, distinta de la pantalla de "Subir archivo de bloqueos".
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearBloqueoManualDTO {

    @NotNull(message = "La fecha y hora de inicio del bloqueo es obligatoria")
    private LocalDateTime fechaHoraInicio;

    @NotNull(message = "La fecha y hora de fin del bloqueo es obligatoria")
    private LocalDateTime fechaHoraFin;

    @NotBlank(message = "Las coordenadas de los tramos bloqueados son obligatorias (formato x1,y1,x2,y2,...)")
    private String coordenadasNodos;
}
