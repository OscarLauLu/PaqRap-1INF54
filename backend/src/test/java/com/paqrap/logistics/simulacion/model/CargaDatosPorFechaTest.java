package com.paqrap.logistics.simulacion.model;

import com.paqrap.logistics.pedidos.model.Pedido;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Las simulaciones toman los pedidos y bloqueos de los archivos oficiales del mes simulado
 * (FAQ 7 y 8), no solo de septiembre 2026.
 */
class CargaDatosPorFechaTest {

    @Test
    void existenArchivosDeVentasYBloqueosDeEnero2026aDiciembre2028() {
        for (YearMonth mes = YearMonth.of(2026, 1); !mes.isAfter(YearMonth.of(2028, 12)); mes = mes.plusMonths(1)) {
            assertTrue(DirectorioDatos.archivoVentas(mes).isPresent(), "falta ventas de " + mes);
            assertTrue(DirectorioDatos.archivoBloqueos(mes).isPresent(), "falta bloqueos de " + mes);
        }
        assertTrue(DirectorioDatos.archivoVentas(YearMonth.of(2029, 1)).isEmpty());
    }

    @Test
    void unaSimulacionDeOctubreCargaPedidosDeOctubre() {
        SimulacionSession sesion = nuevaSesion();
        sesion.cargarDatosHasta(YearMonth.of(2026, 10));

        // ventas.202610 tiene 5,000 líneas; un pedido de más de 24 u se divide, así que hay al menos 5,000
        assertTrue(sesion.getPedidosProgramados().size() >= 5000);
        assertEquals(Set.of(YearMonth.of(2026, 10)), mesesDeRegistro(sesion));
        assertFalse(sesion.getBloqueosProgramados().isEmpty());
        assertEquals(YearMonth.of(2026, 10), sesion.getUltimoMesCargado());
    }

    @Test
    void un5DQueCruzaDeAnioCargaEneroCuandoElRelojLlegaAEse() {
        // 5D desde el 28/12/2026: al iniciar solo diciembre; enero 2027 se agrega al entrar al mes
        SimulacionSession sesion = nuevaSesion();
        sesion.cargarDatosHasta(YearMonth.of(2026, 12));
        assertEquals(Set.of(YearMonth.of(2026, 12)), mesesDeRegistro(sesion));

        sesion.cargarDatosHasta(YearMonth.of(2027, 1));
        assertEquals(Set.of(YearMonth.of(2026, 12), YearMonth.of(2027, 1)), mesesDeRegistro(sesion));

        // Pedir otra vez el mismo mes no lo duplica
        int antes = sesion.getPedidosProgramados().size();
        sesion.cargarDatosHasta(YearMonth.of(2027, 1));
        assertEquals(antes, sesion.getPedidosProgramados().size());
    }

    @Test
    void febreroTienePedidosConFechasValidasDeFebrero() {
        SimulacionSession sesion = nuevaSesion();
        sesion.cargarDatosHasta(YearMonth.of(2026, 2));
        // ventas.202602 tiene 953 líneas
        assertTrue(sesion.getPedidosProgramados().size() >= 953);
        assertEquals(Set.of(YearMonth.of(2026, 2)), mesesDeRegistro(sesion));
    }

    private static SimulacionSession nuevaSesion() {
        SimulacionSession sesion = new SimulacionSession("test-carga-por-fecha");
        sesion.setCargador(new CargadorArchivos());
        return sesion;
    }

    private static Set<YearMonth> mesesDeRegistro(SimulacionSession sesion) {
        return sesion.getPedidosProgramados().stream()
                .map(Pedido::getFechaHoraRegistro)
                .map(YearMonth::from)
                .collect(Collectors.toSet());
    }
}
