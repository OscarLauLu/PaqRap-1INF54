package com.paqrap.logistics.flota.model;

import com.paqrap.logistics.pedidos.model.Pedido;
import com.paqrap.logistics.redvial.model.Ubicacion;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una unidad vehicular de transporte de la flota (RF-39 a RF-50).
 */
@Data
@lombok.EqualsAndHashCode(of = "id")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "unidades_transporte")
public class UnidadTransporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tipo_id", nullable = false)
    private TipoVehiculo tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoOperativo estadoOperativo = EstadoOperativo.DISPONIBLE;

    @Embedded
    private Ubicacion ubicacionActual;

    @Column(name = "carga_actual")
    @Builder.Default
    private int cargaActual = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conductor_id")
    private Conductor conductorAsignado;

    @Builder.Default
    private boolean activo = true;

    @Column(name = "fecha_ultimo_cambio_estado")
    private LocalDateTime fechaUltimoCambioEstado;

    @OneToMany(mappedBy = "unidad", cascade = CascadeType.ALL)
    @ToString.Exclude
    @Builder.Default
    private List<Averia> averias = new ArrayList<>();

    @OneToMany(mappedBy = "unidad", cascade = CascadeType.ALL)
    @ToString.Exclude
    @Builder.Default
    private List<AsignacionTurno> asignacionesTurno = new ArrayList<>();

    /**
     * Calcula la capacidad remanente disponible en paquetes/unidades (RF-07, RF-49).
     */
    public int capacidadDisponible() {
        if (tipo == null) return 0;
        return Math.max(0, tipo.getCapacidadMaxima() - cargaActual);
    }

    /**
     * Evalúa si la unidad puede atender el pedido de forma íntegra sin superar su capacidad máxima (RF-07).
     */
    public boolean puedeAtender(Pedido pedido) {
        if (!activo || estadoOperativo != EstadoOperativo.DISPONIBLE || pedido == null) {
            return false;
        }
        return pedido.getCantidadUnidades() <= capacidadDisponible();
    }

    /**
     * Cambia el estado operativo de la unidad registrando fecha y hora (RF-42).
     */
    public void cambiarEstado(EstadoOperativo nuevo) {
        if (nuevo != null) {
            this.estadoOperativo = nuevo;
            this.fechaUltimoCambioEstado = LocalDateTime.now();
        }
    }

    /**
     * Registra un evento de avería en la unidad y cambia su estado a AVERIADA (RF-14, RF-45).
     */
    public void registrarAveria(Averia averia) {
        if (averia != null) {
            averia.setUnidad(this);
            this.averias.add(averia);
            cambiarEstado(EstadoOperativo.AVERIADA);
        }
    }

    /**
     * Da de baja o retira la unidad de la flota activa, excluyéndola de planificaciones posteriores (RF-47).
     */
    public void darDeBaja() {
        this.activo = false;
        cambiarEstado(EstadoOperativo.EN_MANTENIMIENTO);
    }
}
