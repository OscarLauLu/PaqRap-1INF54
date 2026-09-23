package com.paqrap.logistics.planificacion.model;

import com.paqrap.logistics.almacen.model.Almacen;
import com.paqrap.logistics.flota.model.UnidadTransporte;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
import jakarta.persistence.OrderBy;
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
 * Representa una ruta de entrega planificada (RF-05, RF-06, RF-07, RF-08, RF-55).
 */
@Data
@lombok.EqualsAndHashCode(of = "id")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rutas")
public class Ruta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    private LocalDateTime fechaHoraGeneracion;

    private double distanciaTotalKm;

    private int tiempoEstimadoMin;

    private double costoTotal;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EstadoRuta estado = EstadoRuta.PLANIFICADA;

    @OneToMany(mappedBy = "ruta", cascade = CascadeType.ALL)
    @OrderBy("orden ASC")
    @ToString.Exclude
    @Builder.Default
    private List<ParadaRuta> paradas = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidad_id")
    private UnidadTransporte unidadTransporte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "almacen_id")
    private Almacen almacenOrigen;

    /**
     * Calcula el costo estimado multiplicando la distancia total por la tarifa del vehículo (RF-05).
     * Redondeado a dos decimales en soles.
     */
    public double calcularCosto(double tarifaPorKm) {
        this.costoTotal = Math.round((this.distanciaTotalKm * tarifaPorKm) * 100.0) / 100.0;
        return this.costoTotal;
    }

    /**
     * Calcula el tiempo total estimado sumando tiempos de tránsito y los 60 min de servicio por parada (RF-09).
     */
    public int calcularTiempoEstimado() {
        int tiempoServicioTotal = (paradas != null) ? paradas.size() * 60 : 0;
        // Si hay velocidad del vehículo, el tiempo de tránsito se deriva de la distancia
        int tiempoTransito = 0;
        if (unidadTransporte != null && unidadTransporte.getTipo() != null
                && unidadTransporte.getTipo().getVelocidadPromedioKmH() > 0) {
            double horas = distanciaTotalKm / unidadTransporte.getTipo().getVelocidadPromedioKmH();
            tiempoTransito = (int) Math.ceil(horas * 60.0);
        }
        this.tiempoEstimadoMin = tiempoTransito + tiempoServicioTotal;
        return this.tiempoEstimadoMin;
    }

    /**
     * Agrega una parada a la secuencia de entregas de la ruta.
     */
    public void agregarParada(ParadaRuta parada) {
        if (parada != null) {
            if (this.paradas == null) {
                this.paradas = new ArrayList<>();
            }
            parada.setRuta(this);
            parada.setOrden(this.paradas.size() + 1);
            this.paradas.add(parada);
        }
    }

    /**
     * Valida que la hora estimada de llegada a cada parada no exceda el plazo límite comprometido (RF-08).
     */
    public boolean cumplePlazos() {
        if (paradas == null || paradas.isEmpty()) {
            return true;
        }
        for (ParadaRuta parada : paradas) {
            if (parada.getPedido() != null && parada.getHoraEstimadaLlegada() != null) {
                if (parada.getHoraEstimadaLlegada().isAfter(parada.getPedido().getPlazoLimiteEntrega())) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Valida si el total de paquetes asignados excede la capacidad máxima del vehículo (RF-07).
     */
    public boolean capacidadExcedida() {
        if (unidadTransporte == null || unidadTransporte.getTipo() == null) {
            return false;
        }
        int cargaTotal = 0;
        if (paradas != null) {
            for (ParadaRuta parada : paradas) {
                if (parada.getPedido() != null) {
                    cargaTotal += parada.getPedido().getCantidadUnidades();
                }
            }
        }
        return cargaTotal > unidadTransporte.getTipo().getCapacidadMaxima();
    }
}
