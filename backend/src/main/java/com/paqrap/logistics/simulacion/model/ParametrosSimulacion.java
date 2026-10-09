package com.paqrap.logistics.simulacion.model;

import com.paqrap.logistics.pedidos.model.ConfiguracionSemaforo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Parámetros de configuración previa de una corrida de simulación (RF-64).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParametrosSimulacion {

    @Builder.Default
    private int numAutos = 10;

    @Builder.Default
    private int numMotos = 10;

    @Builder.Default
    private int numBicicletas = 10;

    @Builder.Default
    private int capacidadAlmacenIntermedio = 1000;

    @Builder.Default
    private double tasaIncrementoPedidos = 1.0;

    private ConfiguracionSemaforo rangosSemaforo;

    private String archivoPedidos;

    private String archivoBloqueos;

    private LocalDateTime fechaHoraInicio;

    /**
     * Valida que todos los parámetros configurados cumplan con las restricciones de negocio (RF-64).
     */
    public boolean validar() {
        if (numAutos < 0 || numMotos < 0 || numBicicletas < 0) {
            return false;
        }
        if (numAutos + numMotos + numBicicletas <= 0) {
            return false;
        }
        if (capacidadAlmacenIntermedio <= 0 || capacidadAlmacenIntermedio > 1000) {
            return false;
        }
        if (tasaIncrementoPedidos <= 0.0) {
            return false;
        }
        return true;
    }
}
