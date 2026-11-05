package com.paqrap.logistics.redvial.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bloqueos en la red vial (FAQ 7), con una línea real de datos/bloqueos/bloqueo.2610.txt:
 * "01d07h25m-01d11h16m:15,40,27,40" (calle y = 40 entre x = 15 y x = 27, el 01/10 de 07:25 a 11:16).
 * La red es compartida por la operación y todas las sesiones: cada consulta usa su propia hora.
 */
class RedVialBloqueosTest {

    private static final LocalDateTime DURANTE = LocalDateTime.of(2026, 10, 1, 9, 0);
    private static final LocalDateTime DESPUES = LocalDateTime.of(2026, 10, 1, 12, 0);

    private RedVial red;
    private Bloqueo bloqueo;

    @BeforeEach
    void preparar() {
        red = new RedVial();
        red.inicializarRed();
        bloqueo = Bloqueo.builder()
                .codigo("BLOQ-3")
                .fechaHoraInicio(LocalDateTime.of(2026, 10, 1, 7, 25))
                .fechaHoraFin(LocalDateTime.of(2026, 10, 1, 11, 16))
                .coordenadasNodos("15,40,27,40")
                .build();
        red.aplicarBloqueo(bloqueo);
    }

    @Test
    void soloBloqueaDentroDeSuVentana() {
        assertEquals(12, red.aristasBloqueadasEn(DURANTE).size(), "12 tramos de 1 km entre x=15 y x=27");
        assertTrue(red.estaBloqueadoSegmento(20, 40, 21, 40, DURANTE));
        assertTrue(red.aristasBloqueadasEn(DESPUES).isEmpty());
    }

    @Test
    void aplicarDosVecesElMismoBloqueoNoLoDuplica() {
        red.aplicarBloqueo(bloqueo);
        red.aplicarBloqueo(Bloqueo.builder().codigo("otra-copia").fechaHoraInicio(bloqueo.getFechaHoraInicio())
                .fechaHoraFin(bloqueo.getFechaHoraFin()).coordenadasNodos("15, 40, 27, 40").build());
        assertEquals(12, red.aristasBloqueadasEn(DURANTE.plusMinutes(1)).size());
    }

    @Test
    void laRutaMinimaRodeaElBloqueoSoloMientrasEstaVigente() {
        Nodo desde = red.obtenerNodo(16, 40);
        Nodo hasta = red.obtenerNodo(26, 40);
        // Sin bloqueo: 10 km en línea recta por y = 40
        assertEquals(10, red.calcularRutaMinima(desde, hasta, DESPUES).size());
        // Con bloqueo: debe salir de y = 40 (sube o baja una cuadra y vuelve): 12 km
        List<Tramo> rodeo = red.calcularRutaMinima(desde, hasta, DURANTE);
        assertEquals(12, rodeo.size());
        assertTrue(rodeo.stream().noneMatch(t -> red.estaBloqueado(t, DURANTE)));
    }

    @Test
    void consultasAHorasDistintasNoMezclanSusDistancias() throws Exception {
        Nodo desde = red.obtenerNodo(16, 40);
        // Dos "sesiones" consultando a la vez, una con el bloqueo vigente y otra sin él
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < 50; i++) {
                Future<int[][]> conBloqueo = hilos.submit(() -> red.distanciasDesde(desde, DURANTE));
                Future<int[][]> sinBloqueo = hilos.submit(() -> red.distanciasDesde(desde, DESPUES));
                assertEquals(12, conBloqueo.get()[26][40], "con el bloqueo hay que rodear");
                assertNull(sinBloqueo.get(), "sin bloqueos vale la distancia Manhattan (null)");
            }
        } finally {
            hilos.shutdownNow();
        }
    }
}
