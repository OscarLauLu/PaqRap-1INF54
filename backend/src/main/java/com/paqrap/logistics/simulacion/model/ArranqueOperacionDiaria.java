package com.paqrap.logistics.simulacion.model;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Arranca la Operación día a día apenas se enciende el backend, solo en la instancia de operación
 * (PAQRAP_MODO=OPERACION). Así la operación de la empresa está siempre en marcha y no depende
 * de que alguien la inicie desde la pantalla. La instancia de simulación no hace nada aquí.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArranqueOperacionDiaria implements ApplicationRunner {

    private final MotorSimulacion motor;

    @Override
    public void run(ApplicationArguments args) {
        if (!motor.esModoOperacion()) {
            return;
        }
        try {
            // La operación diaria corre en tiempo real, empezando en la hora actual de Perú
            LocalDateTime ahoraLima = LocalDateTime.now(ZoneId.of("America/Lima")).truncatedTo(ChronoUnit.MINUTES);

            ParametrosSimulacion params = ParametrosSimulacion.builder().build();
            params.setFechaHoraInicio(ahoraLima);
            motor.configurar(params);
            motor.ejecutar(TipoEscenario.DIA_A_DIA);

            log.info("Operación día a día iniciada automáticamente desde {} (hora de Lima)", ahoraLima);
        } catch (Exception e) {
            // Si falla, el backend igual queda en pie y la operación se puede iniciar desde la pantalla
            log.error("No se pudo iniciar automáticamente la operación día a día: {}", e.getMessage());
        }
    }
}