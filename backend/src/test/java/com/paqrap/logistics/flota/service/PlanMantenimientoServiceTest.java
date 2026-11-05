package com.paqrap.logistics.flota.service;

import com.paqrap.logistics.flota.model.Mantenimiento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reglas de mantenimiento preventivo del caso (FAQ 19), con el plan oficial 09.10 del curso
 * y los planes bimestrales 2026-2028 generados a partir de él.
 */
class PlanMantenimientoServiceTest {

    private static final Path DATOS = PlanMantenimientoService.resolverCarpetaDatos();

    private PlanMantenimientoService plan;

    @BeforeEach
    void cargarPlan() {
        plan = new PlanMantenimientoService();
        plan.cargarDesdeCarpeta(DATOS.resolve(PlanMantenimientoService.CARPETA_PLANES));
    }

    @Test
    void planOficialBloqueaCadaUnidadDe0000a2359SinImportarElTipo() {
        List<Mantenimiento> oficial = PlanMantenimientoService.parsearArchivo(DATOS.resolve("mant.preventivo.09.10.txt"));

        // 37 unidades (10 autos, 15 motos, 12 bicicletas): cada una tiene un día en el bimestre
        assertEquals(37, oficial.size());
        assertEquals(37, oficial.stream().map(Mantenimiento::getCodigoVehiculo).distinct().count());

        Map<String, Mantenimiento> porUnidad = oficial.stream()
                .collect(Collectors.toMap(Mantenimiento::getCodigoVehiculo, m -> m));
        for (String codigo : List.of("TA01", "TB01", "TM07")) {   // un auto, una bicicleta, una moto
            Mantenimiento m = porUnidad.get(codigo);
            assertEquals(m.getFechaHoraInicio().toLocalDate().atStartOfDay(), m.getFechaHoraInicio());
            assertEquals(m.getFechaHoraInicio().toLocalDate().atTime(23, 59, 59), m.getFechaHoraFin(),
                    codigo + " debe quedar no disponible solo de 00:00 a 23:59 de su día");
        }
    }

    @Test
    void ta01EstaEnMantenimientoEl01DeSetiembre2026TodoElDia() {
        assertTrue(plan.estaEnMantenimiento("TA01", LocalDateTime.of(2026, 9, 1, 0, 0)));
        assertTrue(plan.estaEnMantenimiento("TA01", LocalDateTime.of(2026, 9, 1, 23, 59, 59)));
        assertFalse(plan.estaEnMantenimiento("TA01", LocalDateTime.of(2026, 8, 31, 23, 59, 59)));
        assertFalse(plan.estaEnMantenimiento("TA01", LocalDateTime.of(2026, 9, 2, 0, 0)));
        assertFalse(plan.estaEnMantenimiento("TA02", LocalDateTime.of(2026, 9, 1, 12, 0)));
    }

    @Test
    void elPlanSeRepiteCadaDosMesesHasta2028() {
        // 6 bimestres x 3 años x 37 unidades
        assertEquals(6 * 3 * 37, plan.getPlan().size());

        // TB01 tiene mantenimiento el 02/09/2026 -> el mismo día del primer mes de cada bimestre
        assertTrue(plan.estaEnMantenimiento("TB01", LocalDateTime.of(2026, 1, 2, 10, 0)));
        assertTrue(plan.estaEnMantenimiento("TB01", LocalDateTime.of(2027, 11, 2, 10, 0)));
        assertTrue(plan.estaEnMantenimiento("TB01", LocalDateTime.of(2028, 5, 2, 10, 0)));
        assertFalse(plan.estaEnMantenimiento("TB01", LocalDateTime.of(2028, 6, 2, 10, 0)));

        // TA10 tiene mantenimiento el 28/10/2026 -> el día 28 del segundo mes, incluso febrero
        assertTrue(plan.estaEnMantenimiento("TA10", LocalDateTime.of(2027, 2, 28, 10, 0)));
        assertTrue(plan.estaEnMantenimiento("TA10", LocalDateTime.of(2028, 12, 28, 10, 0)));
    }

    @Test
    void detectaElMantenimientoQueEmpiezaDuranteUnaRuta() {
        // TA01 entra a mantenimiento el 01/09/2026 a las 00:00
        LocalDateTime desde = LocalDateTime.of(2026, 8, 31, 20, 0);
        assertTrue(plan.mantenimientoQueIniciaEntre("TA01", desde, LocalDateTime.of(2026, 9, 1, 2, 0)).isPresent());
        assertFalse(plan.mantenimientoQueIniciaEntre("TA01", desde, LocalDateTime.of(2026, 8, 31, 23, 0)).isPresent());
        // Un mantenimiento ya empezado no "inicia" dentro del intervalo
        assertFalse(plan.mantenimientoQueIniciaEntre("TA01",
                LocalDateTime.of(2026, 9, 1, 8, 0), LocalDateTime.of(2026, 9, 1, 12, 0)).isPresent());
    }
}
