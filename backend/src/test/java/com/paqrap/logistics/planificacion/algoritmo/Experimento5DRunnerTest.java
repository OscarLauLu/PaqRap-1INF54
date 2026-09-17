package com.paqrap.logistics.planificacion.algoritmo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba unitaria para el ejecutor del diseño experimental 5D.
 */
class Experimento5DRunnerTest {

    @Test
    @DisplayName("Ejecutar piloto rápido del diseño de experimentos 5D")
    void probarPilotoExperimento5D() {
        String testCsv = "test_resultados_piloto_5d.csv";
        String[] args = new String[]{"--piloto", "--output=" + testCsv};
        Experimento5DRunner.main(args);

        File f = new File(testCsv);
        assertTrue(f.exists(), "El archivo CSV de resultados del piloto debe haber sido creado");
        assertTrue(f.length() > 0, "El archivo CSV no debe estar vacío");
        f.deleteOnExit();
    }
}
