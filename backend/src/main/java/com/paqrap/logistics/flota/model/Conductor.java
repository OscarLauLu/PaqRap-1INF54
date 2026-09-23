package com.paqrap.logistics.flota.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa un conductor o repartidor asignado a la flota (RF-43, RF-44).
 * Clave natural (codigo) según docs/62.dis.base.datos.postgresql.v01.md. El turno ya no es un
 * atributo fijo del conductor: se determina por cada {@link AsignacionTurno} (fecha + turno).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "conductor")
public class Conductor {

    @Id
    private String codigo;

    private String nombre;
}
