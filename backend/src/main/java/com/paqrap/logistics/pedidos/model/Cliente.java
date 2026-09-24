package com.paqrap.logistics.pedidos.model;

import com.paqrap.logistics.redvial.model.Ubicacion;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Representa a un cliente del sistema logístico (RF-26).
 * Clave natural (idCliente) según docs/62.dis.base.datos.postgresql.v01.md: ya no existe un
 * "id" numérico paralelo, idCliente ES la clave primaria.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    @Column(name = "id_cliente")
    private String idCliente;

    private String nombre;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "posX", column = @Column(name = "ubicacion_entrega_x")),
            @AttributeOverride(name = "posY", column = @Column(name = "ubicacion_entrega_y"))
    })
    private Ubicacion ubicacionEntrega;

    @OneToMany(mappedBy = "cliente")
    @ToString.Exclude
    @Builder.Default
    private List<Pedido> pedidos = new ArrayList<>();

    public Cliente(String idCliente, String nombre, Ubicacion ubicacionEntrega) {
        this.idCliente = idCliente;
        this.nombre = nombre;
        this.ubicacionEntrega = ubicacionEntrega;
        this.pedidos = new ArrayList<>();
    }

    /**
     * Obtiene el historial de pedidos del cliente con filtros por rango de fechas y estado (RF-37).
     */
    public List<Pedido> obtenerHistorialPedidos(LocalDate desde, LocalDate hasta, EstadoPedido estado) {
        return pedidos.stream()
                .filter(p -> estado == null || p.getEstado() == estado)
                .filter(p -> {
                    if (p.getFechaHoraRegistro() == null) return false;
                    LocalDate fecha = p.getFechaHoraRegistro().toLocalDate();
                    boolean despuesOIgual = (desde == null) || !fecha.isBefore(desde);
                    boolean antesOIgual = (hasta == null) || !fecha.isAfter(hasta);
                    return despuesOIgual && antesOIgual;
                })
                .collect(Collectors.toList());
    }
}
