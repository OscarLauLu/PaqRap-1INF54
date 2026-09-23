package com.paqrap.logistics.almacen.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Representa un movimiento de inventario (carga, descarga, recarga) en un almacén (RF-21, RF-23).
 * Clave natural (codigo) según docs/62.dis.base.datos.postgresql.v01.md.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "movimiento_inventario")
public class MovimientoInventario {

    @Id
    private String codigo;

    private LocalDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    private TipoMovimiento tipo;

    private int cantidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "almacen_codigo")
    private Almacen almacen;

    private String codigoRuta;

    /**
     * FK diferida a "ruta" (tabla fuera del alcance de este remodelado); se persiste como
     * columna simple, no como relación JPA, para no arrastrar cambios a planificacion.model.Ruta.
     */
    @jakarta.persistence.Column(name = "ruta_codigo")
    private String rutaCodigo;

    private String descripcion;
}
