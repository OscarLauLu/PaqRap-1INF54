package com.paqrap.logistics.flota.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Catálogo de los 3 turnos de trabajo oficiales para conductores (RF-43, RF-44).
 * Según docs/62.dis.base.datos.postgresql.v01.md, "turno" es una tabla catálogo real de 3 filas
 * (antes era un value object embebido en Conductor); cada {@link AsignacionTurno} referencia un
 * turno por fecha, en vez de que el conductor tenga un turno fijo.
 * Turnos estándar:
 * - Mañana: 07:00 a 15:00
 * - Tarde: 15:00 a 23:00
 * - Noche: 23:00 a 07:00 (cruza medianoche)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "turno")
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "hora_fin")
    private LocalTime horaFin;

    /**
     * Calcula la duración del turno en horas.
     */
    public int duracionHoras() {
        if (horaInicio == null || horaFin == null) return 0;
        if (horaFin.isAfter(horaInicio)) {
            return (int) Duration.between(horaInicio, horaFin).toHours();
        } else {
            // Cruza medianoche (ej: 23:00 a 07:00)
            return (int) (Duration.between(horaInicio, LocalTime.MAX).toHours() + 1
                    + Duration.between(LocalTime.MIN, horaFin).toHours());
        }
    }

    /**
     * Determina si un instante temporal dado está dentro de este turno.
     */
    public boolean contiene(LocalDateTime instante) {
        if (instante == null || horaInicio == null || horaFin == null) return false;
        LocalTime hora = instante.toLocalTime();

        if (!horaFin.isBefore(horaInicio)) {
            // Turno dentro del mismo día (ej. 07:00 - 15:00)
            return !hora.isBefore(horaInicio) && hora.isBefore(horaFin);
        } else {
            // Turno nocturno que cruza medianoche (ej. 23:00 - 07:00)
            return !hora.isBefore(horaInicio) || hora.isBefore(horaFin);
        }
    }

    public static Turno turnoManana() {
        return Turno.builder().horaInicio(LocalTime.of(7, 0)).horaFin(LocalTime.of(15, 0)).build();
    }

    public static Turno turnoTarde() {
        return Turno.builder().horaInicio(LocalTime.of(15, 0)).horaFin(LocalTime.of(23, 0)).build();
    }

    public static Turno turnoNoche() {
        return Turno.builder().horaInicio(LocalTime.of(23, 0)).horaFin(LocalTime.of(7, 0)).build();
    }
}
