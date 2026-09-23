package com.paqrap.logistics.flota.model;

import com.paqrap.logistics.redvial.model.Ubicacion;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Representa una avería o falla mecánica en una unidad de transporte (RF-14, RF-45, RF-61).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "averias")
public class Averia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codigo;

    @Enumerated(EnumType.STRING)
    private TipoAveria tipo;

    private LocalDateTime fechaHoraEvento;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "posX", column = @Column(name = "ubicacion_falla_x")),
            @AttributeOverride(name = "posY", column = @Column(name = "ubicacion_falla_y"))
    })
    private Ubicacion ubicacionFalla;

    private LocalDateTime horaReincorporacion;

    @Builder.Default
    private boolean origenManual = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidad_codigo")
    private UnidadTransporte unidad;

    @Builder.Default
    private boolean resuelta = false;

    /**
     * Calcula automáticamente la hora estimada de reincorporación según la regla de negocio (RF-14):
     * - Tipo 1: 2 horas después del evento.
     * - Tipo 2: Lo que ocurra primero entre el fin del siguiente cambio de turno (07:00, 15:00 o 23:00) y un máximo de 4 horas en el lugar.
     * - Tipo 3: Al menos 2 días después, reincorporándose al inicio del turno 15:00-23:00.
     *
     * @param turnoActual Turno activo al momento de la falla (opcional)
     * @return LocalDateTime estimado de reincorporación
     */
    public LocalDateTime calcularReincorporacion(Turno turnoActual) {
        if (fechaHoraEvento == null || tipo == null) {
            return null;
        }

        switch (tipo) {
            case TIPO_1:
                return fechaHoraEvento.plusHours(2);

            case TIPO_2:
                LocalDateTime cuatroHorasDespues = fechaHoraEvento.plusHours(4);
                LocalDateTime proximoCambioTurno = calcularProximoCambioTurno(fechaHoraEvento);
                if (proximoCambioTurno.isBefore(cuatroHorasDespues)) {
                    return proximoCambioTurno;
                } else {
                    return cuatroHorasDespues;
                }

            case TIPO_3:
                // Al menos 2 días después, inicio del turno 15:00-23:00
                LocalDate fechaFutura = fechaHoraEvento.toLocalDate().plusDays(2);
                LocalDateTime reincorp = LocalDateTime.of(fechaFutura, LocalTime.of(15, 0));
                if (reincorp.isBefore(fechaHoraEvento.plusDays(2))) {
                    reincorp = reincorp.plusDays(1);
                }
                return reincorp;

            default:
                return fechaHoraEvento.plusHours(2);
        }
    }

    private LocalDateTime calcularProximoCambioTurno(LocalDateTime momento) {
        LocalDate fecha = momento.toLocalDate();
        LocalTime hora = momento.toLocalTime();

        LocalTime t1 = LocalTime.of(7, 0);
        LocalTime t2 = LocalTime.of(15, 0);
        LocalTime t3 = LocalTime.of(23, 0);

        if (hora.isBefore(t1)) {
            return LocalDateTime.of(fecha, t1);
        } else if (hora.isBefore(t2)) {
            return LocalDateTime.of(fecha, t2);
        } else if (hora.isBefore(t3)) {
            return LocalDateTime.of(fecha, t3);
        } else {
            return LocalDateTime.of(fecha.plusDays(1), t1);
        }
    }
}
