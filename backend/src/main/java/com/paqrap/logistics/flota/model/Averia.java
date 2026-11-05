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
     * - Tipo 2: hasta el final del turno siguiente al de la avería (FAQ 3). Ej.: avería a las 10:00
     *   (turno 07-15) -> el turno siguiente es 15-23 -> se reincorpora a las 23:00. En el lugar
     *   permanece 4 h como máximo (ver {@link #horasEnElLugar()}).
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
                // Fin del turno actual + un turno completo (8 h) = fin del turno siguiente
                return calcularProximoCambioTurno(fechaHoraEvento).plusHours(8);

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

    /**
     * Horas que la unidad permanece inmóvil en el lugar de la avería (FAQ 3): tipo 1, sus 2 h de
     * reparación; tipo 2 y 3, 4 h, y luego se traslada de forma instantánea al almacén central.
     */
    public int horasEnElLugar() {
        return (tipo == TipoAveria.TIPO_1) ? 2 : 4;
    }

    /** true si tras su tiempo en el lugar la unidad se traslada al almacén central (tipos 2 y 3). */
    public boolean seTrasladaAlCentral() {
        return tipo == TipoAveria.TIPO_2 || tipo == TipoAveria.TIPO_3;
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
