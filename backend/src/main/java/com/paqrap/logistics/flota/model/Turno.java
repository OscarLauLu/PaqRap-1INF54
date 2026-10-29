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
        return Turno.builder().id(1).horaInicio(LocalTime.of(7, 0)).horaFin(LocalTime.of(15, 0)).build();
    }

    public static Turno turnoTarde() {
        return Turno.builder().id(2).horaInicio(LocalTime.of(15, 0)).horaFin(LocalTime.of(23, 0)).build();
    }

    public static Turno turnoNoche() {
        return Turno.builder().id(3).horaInicio(LocalTime.of(23, 0)).horaFin(LocalTime.of(7, 0)).build();
    }

    /**
     * Retorna el turno oficial (1, 2 o 3) activo para una hora dada (RF-43).
     */
    public static Turno obtenerTurnoActivo(LocalTime hora) {
        if (hora == null) return turnoManana();
        if (!hora.isBefore(LocalTime.of(7, 0)) && hora.isBefore(LocalTime.of(15, 0))) {
            return turnoManana();
        } else if (!hora.isBefore(LocalTime.of(15, 0)) && hora.isBefore(LocalTime.of(23, 0))) {
            return turnoTarde();
        } else {
            return turnoNoche();
        }
    }

    /**
     * Retorna la hora de inicio de alimentación por defecto (60 min) cumpliendo
     * con estar al menos 1h después del inicio y 1h antes del fin de turno (RF-44).
     */
    public LocalTime getHoraInicioAlimentacion() {
        if (horaInicio != null && horaInicio.equals(LocalTime.of(7, 0))) {
            return LocalTime.of(12, 0); // Mañana: 12:00 a 13:00
        } else if (horaInicio != null && horaInicio.equals(LocalTime.of(15, 0))) {
            return LocalTime.of(19, 0); // Tarde: 19:00 a 20:00
        } else {
            return LocalTime.of(3, 0);  // Noche: 03:00 a 04:00
        }
    }

    /**
     * Determina si una hora específica coincide con la ventana de alimentación obligatoria (RF-44).
     */
    public static boolean esHoraAlimentacion(LocalTime hora) {
        if (hora == null) return false;
        // Almuerzo: 12:00 a 13:00
        if (!hora.isBefore(LocalTime.of(12, 0)) && hora.isBefore(LocalTime.of(13, 0))) return true;
        // Cena: 19:00 a 20:00
        if (!hora.isBefore(LocalTime.of(19, 0)) && hora.isBefore(LocalTime.of(20, 0))) return true;
        // Refrigerio noche: 03:00 a 04:00
        if (!hora.isBefore(LocalTime.of(3, 0)) && hora.isBefore(LocalTime.of(4, 0))) return true;
        return false;
    }

    /**
     * Ajusta el instante proyectado si el inicio o el intervalo se cruza con la hora de alimentación obligatoria (RF-44).
     */
    public static LocalDateTime ajustarPorAlimentacion(LocalDateTime inicio, long minutosDuracion) {
        if (inicio == null) return null;
        LocalDateTime tiempoActual = inicio;
        if (esHoraAlimentacion(tiempoActual.toLocalTime())) {
            int minRestantes = 60 - tiempoActual.getMinute();
            tiempoActual = tiempoActual.plusMinutes(minRestantes);
        }
        LocalDateTime fin = tiempoActual.plusMinutes(minutosDuracion);
        LocalTime hIni = tiempoActual.toLocalTime();
        LocalTime hFin = fin.toLocalTime();
        boolean cruzo12 = hIni.isBefore(LocalTime.of(12, 0)) && !hFin.isBefore(LocalTime.of(12, 0));
        boolean cruzo19 = hIni.isBefore(LocalTime.of(19, 0)) && !hFin.isBefore(LocalTime.of(19, 0));
        boolean cruzo03 = hIni.isBefore(LocalTime.of(3, 0)) && !hFin.isBefore(LocalTime.of(3, 0));
        if (cruzo12 || cruzo19 || cruzo03 || esHoraAlimentacion(hFin)) {
            fin = fin.plusMinutes(60);
        }
        return fin;
    }
}
