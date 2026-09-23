package com.paqrap.logistics.simulacion.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Gestiona el avance del tiempo simulado y el factor de aceleración temporal (RF-71).
 */
@Component
public class RelojSimulado {

    @Getter
    @Setter
    private LocalDateTime instanteInicio;

    @Getter
    @Setter
    private volatile LocalDateTime instanteActual;

    @Getter
    @Setter
    private double factorAceleracion = 1.0;

    public RelojSimulado() {
        this.instanteInicio = LocalDateTime.of(2026, 9, 1, 0, 0, 0);
        this.instanteActual = this.instanteInicio;
    }

    public void inicializar(LocalDateTime inicio, double aceleracion) {
        this.instanteInicio = inicio != null ? inicio : LocalDateTime.of(2026, 9, 1, 0, 0, 0);
        this.instanteActual = this.instanteInicio;
        this.factorAceleracion = aceleracion > 0 ? aceleracion : 1.0;
    }

    /**
     * Avanza el reloj una cantidad específica de minutos simulados.
     */
    public void avanzar(int minutos) {
        if (instanteActual != null) {
            instanteActual = instanteActual.plusMinutes(minutos);
        }
    }

    /**
 * Avanza el reloj según el tiempo real transcurrido, escalado por el factor de aceleración.
 * factorAceleracion = segundos simulados por segundo real.
 */
public void avanzar(java.time.Duration tiempoReal) {
    if (instanteActual != null && tiempoReal != null) {
        double segundosSimulados = (tiempoReal.toMillis() / 1000.0) * factorAceleracion;
        instanteActual = instanteActual.plusNanos((long) (segundosSimulados * 1_000_000_000L));
    }
}

    /**
     * Retorna el número de día transcurrido desde el inicio de la simulación (Día 1, Día 2, etc.).
     */
    public int diaSimulado() {
        if (instanteInicio == null || instanteActual == null) return 1;
        long dias = Duration.between(instanteInicio, instanteActual).toDays();
        return (int) dias + 1;
    }

    /**
     * Retorna la hora del reloj en el instante simulado (RF-71).
     */
    public LocalTime horaSimulada() {
        return instanteActual != null ? instanteActual.toLocalTime() : LocalTime.MIDNIGHT;
    }

    public String formatoReloj() {
        return String.format("Día %d - %02d:%02d:%02d",
                diaSimulado(),
                horaSimulada().getHour(),
                horaSimulada().getMinute(),
                horaSimulada().getSecond());
    }
}
