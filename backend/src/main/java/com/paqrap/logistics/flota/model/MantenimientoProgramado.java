package com.paqrap.logistics.flota.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Día de mantenimiento preventivo de una unidad (FAQ 19, RF-42). Mapea a la tabla
 * {@code mantenimiento_programado} de docs/62 (codigo, unidad, fecha, hora_inicio, hora_fin).
 *
 * <p>{@code unidad} es una FK real a {@code unidad_transporte}: un mantenimiento solo puede
 * registrarse para una unidad existente en la flota.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mantenimiento_programado",
        indexes = @Index(name = "ix_mantenimiento_unidad_fecha", columnList = "unidad_codigo, fecha"))
public class MantenimientoProgramado {

    /** Clave natural: {@code MNT-aaaammdd-TTNN}. */
    @Id
    @Column(length = 40)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidad_codigo", nullable = false,
            foreignKey = @ForeignKey(name = "fk_mantenimiento_unidad"))
    private UnidadTransporte unidad;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;
}
