package com.paqrap.logistics.flota.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Representa la tipología y parámetros operacionales de un tipo de vehículo (RF-39, RF-40, RF-41, RF-46).
 * Clave natural según docs/62.dis.base.datos.postgresql.v01.md: codigo de 2 letras (TA, TM, TB).
 * Admite actualización en caliente durante la ejecución del sistema:
 * - Auto (TA): 24 u | 40 km/h | S/8.00/km
 * - Moto (TM): 8 u | 25 km/h | S/6.00/km
 * - Bicicleta (TB): 4 u | 12 km/h | S/3.00/km
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tipo_vehiculo")
public class TipoVehiculo {

    @Id
    @Column(length = 2)
    private String codigo;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "capacidad_maxima")
    private int capacidadMaxima;

    @Column(name = "velocidad_promedio_kmh")
    private double velocidadPromedioKmH;

    @Column(name = "costo_por_km")
    private double costoPorKm;

    @Column(name = "actualizado_en")
    @Builder.Default
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    public TipoVehiculo(String codigo, String nombre, int capacidadMaxima, double velocidadPromedioKmH, double costoPorKm) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.capacidadMaxima = capacidadMaxima;
        this.velocidadPromedioKmH = velocidadPromedioKmH;
        this.costoPorKm = costoPorKm;
        this.actualizadoEn = LocalDateTime.now();
    }

    /**
     * Actualiza la velocidad promedio configurada (cambio en caliente, RF-40).
     */
    public void actualizarVelocidad(double nueva) {
        if (nueva <= 0) {
            throw new IllegalArgumentException("La velocidad debe ser mayor a cero");
        }
        this.velocidadPromedioKmH = nueva;
        this.actualizadoEn = LocalDateTime.now();
    }

    /**
     * Actualiza el costo por kilómetro configurado (cambio en caliente, RF-41).
     */
    public void actualizarCosto(double nuevo) {
        if (nuevo < 0) {
            throw new IllegalArgumentException("El costo no puede ser negativo");
        }
        this.costoPorKm = nuevo;
        this.actualizadoEn = LocalDateTime.now();
    }

    /**
     * Actualiza la capacidad máxima del tipo de vehículo (RF-46).
     */
    public void actualizarCapacidad(int nueva) {
        if (nueva <= 0) {
            throw new IllegalArgumentException("La capacidad máxima debe ser un entero mayor a cero");
        }
        this.capacidadMaxima = nueva;
        this.actualizadoEn = LocalDateTime.now();
    }
}
