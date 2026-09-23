package com.paqrap.logistics.flota.model;

import com.paqrap.logistics.pedidos.model.Pedido;
import com.paqrap.logistics.redvial.model.Ubicacion;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Representa una unidad vehicular de transporte de la flota (RF-39 a RF-50).
 * Clave natural (codigo) según docs/62.dis.base.datos.postgresql.v01.md. La asignación de
 * conductor ya no es una FK fija en esta entidad: se modela por fecha/turno en
 * {@link AsignacionTurno}. El campo persistido "dadaDeBaja" reemplaza a "activo" (semántica
 * invertida: dadaDeBaja=true significa fuera de flota activa). Se conservan isActivo()/
 * setActivo(boolean) como métodos de conveniencia (no mapeados por JPA) para no romper el
 * código de planificación/algoritmos que solo necesita leer disponibilidad.
 */
@Data
@lombok.EqualsAndHashCode(of = "codigo")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "unidad_transporte")
public class UnidadTransporte {

    @Id
    private String codigo;

    @ManyToOne(fetch = jakarta.persistence.FetchType.EAGER)
    @JoinColumn(name = "tipo_vehiculo_codigo", nullable = false)
    private TipoVehiculo tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoOperativo estadoOperativo = EstadoOperativo.DISPONIBLE;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "posX", column = @Column(name = "ubicacion_actual_x")),
            @AttributeOverride(name = "posY", column = @Column(name = "ubicacion_actual_y"))
    })
    private Ubicacion ubicacionActual;

    @Column(name = "carga_actual")
    @Builder.Default
    private int cargaActual = 0;

    @Column(name = "dada_de_baja")
    @Builder.Default
    private boolean dadaDeBaja = false;

    @OneToMany(mappedBy = "unidad", cascade = CascadeType.ALL)
    @ToString.Exclude
    @Builder.Default
    private List<Averia> averias = new ArrayList<>();

    @OneToMany(mappedBy = "unidad", cascade = CascadeType.ALL)
    @ToString.Exclude
    @Builder.Default
    private List<AsignacionTurno> asignacionesTurno = new ArrayList<>();

    /**
     * Conveniencia de lectura equivalente al antiguo campo "activo" (no persistida: refleja
     * !dadaDeBaja). Mantiene compatible el código de ACO/ALNS que solo lee disponibilidad.
     */
    public boolean isActivo() {
        return !dadaDeBaja;
    }

    /**
     * Conveniencia de escritura equivalente al antiguo setter "activo" (no persistida: escribe
     * sobre dadaDeBaja invirtiendo el valor).
     */
    public void setActivo(boolean activo) {
        this.dadaDeBaja = !activo;
    }

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
        if (dadaDeBaja || estadoOperativo != EstadoOperativo.DISPONIBLE || pedido == null) {
            return false;
        }
        return pedido.getCantidadUnidades() <= capacidadDisponible();
    }

    /**
     * Cambia el estado operativo de la unidad (RF-42).
     */
    public void cambiarEstado(EstadoOperativo nuevo) {
        if (nuevo != null) {
            this.estadoOperativo = nuevo;
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
        this.dadaDeBaja = true;
        cambiarEstado(EstadoOperativo.EN_MANTENIMIENTO);
    }
}
