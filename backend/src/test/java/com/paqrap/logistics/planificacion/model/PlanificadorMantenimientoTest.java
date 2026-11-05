package com.paqrap.logistics.planificacion.model;

import com.paqrap.logistics.flota.model.UnidadTransporte;
import com.paqrap.logistics.flota.service.PlanMantenimientoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FAQ 19, nota 2: una unidad no debe estar en ruta a las 00:00 de su día de mantenimiento;
 * la planificación debe evitarlo. TA01 entra a mantenimiento el 01/09/2026.
 */
class PlanificadorMantenimientoTest {

    private Planificador planificador;

    @BeforeEach
    void preparar() {
        PlanMantenimientoService plan = new PlanMantenimientoService();
        Path datos = Files.isDirectory(Paths.get("datos")) ? Paths.get("datos") : Paths.get("..", "datos");
        plan.cargarDesdeCarpeta(datos.resolve("mantenimiento"));
        planificador = new Planificador(null, null, null, null, null, null);
        planificador.setPlanMantenimiento(plan);
    }

    /** Ruta de TA01 con una sola parada: llega 3 h después de salir y se queda 1 h (acondicionamiento). */
    private Ruta rutaDeTresHorasMasUnaDeServicio(LocalDateTime base) {
        ParadaRuta parada = ParadaRuta.builder()
                .horaEstimadaLlegada(base.plusHours(3))
                .tiempoServicioMin(60)
                .build();
        return Ruta.builder()
                .fechaHoraGeneracion(base)
                .unidadTransporte(UnidadTransporte.builder().codigo("TA01").build())
                .paradas(List.of(parada))
                .build();
    }

    @Test
    void rechazaRutaQueTerminariaDespuesDeLas0000DelDiaDeMantenimiento() {
        // El algoritmo puede fechar la ruta con su propio reloj: solo importa la duración (4 h)
        Ruta ruta = rutaDeTresHorasMasUnaDeServicio(LocalDateTime.of(2026, 8, 25, 6, 0));
        // Planificada a las 21:00 del 31/08 -> terminaría a la 01:00 del 01/09
        assertTrue(planificador.cruzaMantenimiento(ruta, LocalDateTime.of(2026, 8, 25, 6, 0),
                LocalDateTime.of(2026, 8, 31, 21, 0)));
    }

    @Test
    void aceptaRutaQueTerminaAntesDeLas0000() {
        Ruta ruta = rutaDeTresHorasMasUnaDeServicio(LocalDateTime.of(2026, 8, 25, 6, 0));
        // Planificada a las 18:00 del 31/08 -> termina a las 22:00, antes del mantenimiento
        assertFalse(planificador.cruzaMantenimiento(ruta, LocalDateTime.of(2026, 8, 25, 6, 0),
                LocalDateTime.of(2026, 8, 31, 18, 0)));
    }

    @Test
    void otraUnidadSinMantenimientoEsaNocheNoSeRechaza() {
        Ruta ruta = rutaDeTresHorasMasUnaDeServicio(LocalDateTime.of(2026, 8, 25, 6, 0));
        ruta.setUnidadTransporte(UnidadTransporte.builder().codigo("TA02").build());
        assertFalse(planificador.cruzaMantenimiento(ruta, LocalDateTime.of(2026, 8, 25, 6, 0),
                LocalDateTime.of(2026, 8, 31, 21, 0)));
    }
}
