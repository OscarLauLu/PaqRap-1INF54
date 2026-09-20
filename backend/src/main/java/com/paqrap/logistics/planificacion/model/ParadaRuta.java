package com.paqrap.logistics.planificacion.model;

import com.paqrap.logistics.pedidos.model.Pedido;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Representa una parada individual dentro de un itinerario de ruta (RF-06, RF-09, RF-55).
 * Cada parada incluye un tiempo fijo de servicio de 60 minutos (RF-09).
 */
@Data
@lombok.EqualsAndHashCode(of = "id")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "paradas_ruta")
public class ParadaRuta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int orden;

    private LocalDateTime horaEstimadaLlegada;

    @Builder.Default
    private int tiempoServicioMin = 60;

    @Builder.Default
    private boolean entregada = false;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ruta_id")
    private Ruta ruta;

    /**
     * Registra formalmente la entrega del pedido en el punto de parada (RF-33).
     */
    public void registrarEntrega(LocalDateTime instante) {
        this.entregada = true;
        if (this.pedido != null) {
            this.pedido.confirmarEntrega(instante);
        }
    }
}
