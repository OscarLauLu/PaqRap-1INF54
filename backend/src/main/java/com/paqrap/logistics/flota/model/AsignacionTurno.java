package com.paqrap.logistics.flota.model;

import com.paqrap.logistics.redvial.model.Ubicacion;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Representa la asignación de un conductor a una unidad para un turno y fecha específicos (RF-43, RF-44).
 * Tabla "asignacion_turno" (antes "asignaciones_turno") según docs/62.dis.base.datos.postgresql.v01.md.
 * "turno" pasa de value object embebido a FK contra el catálogo real {@link Turno}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "asignacion_turno")
public class AsignacionTurno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conductor_codigo")
    private Conductor conductor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidad_codigo")
    private UnidadTransporte unidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turno_id")
    private Turno turno;

    private LocalDate fecha;

    private LocalTime horaInicioAlimentacion;

    @Builder.Default
    private int duracionAlimentacionMin = 60;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "posX", column = @Column(name = "ubicacion_relevo_x")),
            @AttributeOverride(name = "posY", column = @Column(name = "ubicacion_relevo_y"))
    })
    private Ubicacion ubicacionRelevo;

    /**
     * Valida que el conductor esté asignado a uno de los 3 turnos oficiales (RF-44).
     */
    public boolean validarHorario() {
        if (turno == null || turno.getHoraInicio() == null || turno.getHoraFin() == null) {
            return false;
        }
        LocalTime ini = turno.getHoraInicio();
        LocalTime fin = turno.getHoraFin();

        boolean esTurno1 = ini.equals(LocalTime.of(7, 0)) && fin.equals(LocalTime.of(15, 0));
        boolean esTurno2 = ini.equals(LocalTime.of(15, 0)) && fin.equals(LocalTime.of(23, 0));
        boolean esTurno3 = ini.equals(LocalTime.of(23, 0)) && fin.equals(LocalTime.of(7, 0));

        return esTurno1 || esTurno2 || esTurno3;
    }

    /**
     * Valida que la hora de alimentación obligatoria sea de 60 minutos y esté ubicada
     * al menos 60 minutos después del inicio y antes del fin del turno (RF-44).
     */
    public boolean validarAlimentacion() {
        if (horaInicioAlimentacion == null || turno == null || duracionAlimentacionMin < 60) {
            return false;
        }

        LocalTime inicioTurno = turno.getHoraInicio();
        LocalTime finTurno = turno.getHoraFin();
        LocalTime finAlimentacion = horaInicioAlimentacion.plusMinutes(duracionAlimentacionMin);

        // Turno sin cruce de medianoche
        if (!finTurno.isBefore(inicioTurno)) {
            LocalTime minPermitido = inicioTurno.plusMinutes(60);
            LocalTime maxPermitidoFin = finTurno.minusMinutes(0);
            return !horaInicioAlimentacion.isBefore(minPermitido) && !finAlimentacion.isAfter(finTurno);
        } else {
            // Turno nocturno 23:00 a 07:00
            // Mínimo permitido: 23:00 + 60 min = 00:00 (o medianoche)
            // Fin de alimentación debe ser antes de 07:00
            boolean enTramoAntesMedianoche = !horaInicioAlimentacion.isBefore(inicioTurno.plusMinutes(60));
            boolean enTramoDespuesMedianoche = horaInicioAlimentacion.isBefore(finTurno.minusMinutes(duracionAlimentacionMin));
            return enTramoAntesMedianoche || enTramoDespuesMedianoche;
        }
    }
}
