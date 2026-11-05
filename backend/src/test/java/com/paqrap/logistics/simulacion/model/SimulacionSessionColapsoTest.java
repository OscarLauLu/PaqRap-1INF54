package com.paqrap.logistics.simulacion.model;

import com.paqrap.logistics.pedidos.model.EstadoPedido;
import com.paqrap.logistics.pedidos.model.Pedido;
import com.paqrap.logistics.pedidos.model.TipoEntrega;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Colapso logístico: el sistema colapsa en el instante en que UN SOLO pedido incumple su plazo
 * límite; sin umbral ni porcentaje. La sesión debe informar qué pedido lo provocó.
 */
class SimulacionSessionColapsoTest {

    private static final LocalDateTime REGISTRO = LocalDateTime.of(2026, 10, 12, 8, 0);

    private SimulacionSession sesionEn(LocalDateTime instante) {
        SimulacionSession sesion = new SimulacionSession("test-colapso");
        sesion.getReloj().inicializar(instante, 1.0);
        return sesion;
    }

    private Pedido pedido(String codigo, TipoEntrega tipo, EstadoPedido estado) {
        return Pedido.builder()
                .codigo(codigo)
                .cantidadUnidades(10)
                .fechaHoraRegistro(REGISTRO)
                .tipoEntrega(tipo)
                .plazoLimiteEntrega(REGISTRO.plusHours(tipo.getHorasPlazo()))
                .estado(estado)
                .build();
    }

    @Test
    void sinPedidosVencidosNoHayColapso() {
        // Pedido de 4 h registrado a las 08:00; a las 11:59 todavía está en plazo
        SimulacionSession sesion = sesionEn(REGISTRO.plusHours(3).plusMinutes(59));
        sesion.getPedidosActivos().add(pedido("PED-A", TipoEntrega.PRIORIZADA_4H, EstadoPedido.REGISTRADO));

        assertFalse(sesion.detectarColapso());
        assertNull(sesion.getCodigoPedidoColapso());
    }

    @Test
    void unSoloPedidoVencidoProvocaElColapsoEInformaSuCodigo() {
        // Entre 200 pedidos en plazo, uno de 4 h sin entregar a las 12:01 (1 min tarde) basta
        SimulacionSession sesion = sesionEn(REGISTRO.plusHours(4).plusMinutes(1));
        for (int i = 0; i < 200; i++) {
            sesion.getPedidosActivos().add(pedido("PED-" + i, TipoEntrega.REGULAR_36H, EstadoPedido.PLANIFICADO));
        }
        sesion.getPedidosActivos().add(pedido("PED-VENCIDO", TipoEntrega.PRIORIZADA_4H, EstadoPedido.PLANIFICADO));

        assertTrue(sesion.detectarColapso());
        assertEquals("PED-VENCIDO", sesion.getCodigoPedidoColapso());
        assertEquals(201, sesion.contarPedidosActivos());
    }

    @Test
    void unPedidoYaEntregadoNoProvocaColapsoAunqueSuPlazoHayaPasado() {
        SimulacionSession sesion = sesionEn(REGISTRO.plusHours(10));
        sesion.getPedidosActivos().add(pedido("PED-OK", TipoEntrega.PRIORIZADA_4H, EstadoPedido.ENTREGADO));

        assertFalse(sesion.detectarColapso());
    }

    @Test
    void unaEntregaTardiaTambienEsColapso() {
        SimulacionSession sesion = sesionEn(REGISTRO);
        sesion.setPedidosTarde(1);

        assertTrue(sesion.detectarColapso());
    }
}
