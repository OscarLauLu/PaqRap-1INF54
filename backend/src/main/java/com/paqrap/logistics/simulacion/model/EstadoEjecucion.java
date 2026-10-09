package com.paqrap.logistics.simulacion.model;

import lombok.Getter;

/**
 * Estados del ciclo de vida del motor de simulación.
 */
@Getter
public enum EstadoEjecucion {
    CONFIGURADA("Configurada"),
    EN_EJECUCION("En Ejecución"),
    PAUSADA("Pausada"),
    FINALIZADA("Finalizada con Éxito"),
    DETENIDA_POR_COLAPSO("Detenida por Colapso Logístico");

    private final String descripcion;

    EstadoEjecucion(String descripcion) {
        this.descripcion = descripcion;
    }
}
