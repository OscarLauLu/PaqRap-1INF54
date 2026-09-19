package com.paqrap.logistics.pedidos.model;

import com.paqrap.logistics.redvial.model.Ubicacion;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Representa un pedido registrado en el sistema logístico (RF-26 a RF-38).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private int cantidadUnidades;

    @Column(nullable = false)
    private LocalDateTime fechaHoraRegistro;

    @Column(nullable = false)
    private LocalDateTime plazoLimiteEntrega;

    private LocalDateTime fechaHoraEntrega;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoPedido estado = EstadoPedido.REGISTRADO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEntrega tipoEntrega;

    @Enumerated(EnumType.STRING)
    private NivelCriticidad nivelCriticidad;

    @Embedded
    private Ubicacion destino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    private Long unidadAsignadaId;
    private Long rutaAsignadaId;
    private String codigoPadre;

    @PrePersist
    public void prePersist() {
        if (fechaHoraRegistro == null) {
            fechaHoraRegistro = LocalDateTime.now();
        }
        if (plazoLimiteEntrega == null && tipoEntrega != null) {
            plazoLimiteEntrega = calcularPlazoLimite();
        }
    }

    /**
     * Calcula el plazo límite de entrega sumando las horas del tipo de entrega a la fecha de registro (RF-30).
     */
    public LocalDateTime calcularPlazoLimite() {
        if (fechaHoraRegistro == null || tipoEntrega == null) {
            return null;
        }
        return fechaHoraRegistro.plusHours(tipoEntrega.getHorasPlazo());
    }

    /**
     * Calcula el tiempo restante hasta el plazo comprometido del pedido (RF-03).
     */
    public Duration calcularHolgura(LocalDateTime instanteActual) {
        if (plazoLimiteEntrega == null || instanteActual == null) {
            return Duration.ZERO;
        }
        return Duration.between(instanteActual, plazoLimiteEntrega);
    }

    /**
     * Evalúa y clasifica el pedido en los tres niveles de criticidad del semáforo (RF-03, RF-53).
     */
    public NivelCriticidad evaluarCriticidad(ConfiguracionSemaforo config) {
        if (config == null) {
            return NivelCriticidad.VERDE;
        }
        Duration holgura = calcularHolgura(LocalDateTime.now());
        this.nivelCriticidad = config.clasificar(holgura);
        return this.nivelCriticidad;
    }

    /**
     * Determina si el pedido puede ser editado o cancelado (solo en estado REGISTRADO, RF-35, RF-36).
     */
    public boolean esModificable() {
        return this.estado == EstadoPedido.REGISTRADO;
    }

    /**
     * Cancela el pedido liberando inventario reservado (RF-36).
     */
    public void cancelar() {
        if (!esModificable()) {
            throw new IllegalStateException("No se puede cancelar un pedido con estado: " + estado);
        }
        this.estado = EstadoPedido.CANCELADO;
    }

    /**
     * Confirma la entrega efectiva del pedido en el instante indicado (RF-33).
     */
    public void confirmarEntrega(LocalDateTime instante) {
        this.estado = EstadoPedido.ENTREGADO;
        this.fechaHoraEntrega = instante;
    }
}
