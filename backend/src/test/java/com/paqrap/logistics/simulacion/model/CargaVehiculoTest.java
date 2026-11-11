package com.paqrap.logistics.simulacion.model;

import com.paqrap.logistics.flota.model.EstadoOperativo;
import com.paqrap.logistics.flota.model.TipoVehiculo;
import com.paqrap.logistics.flota.model.UnidadTransporte;
import com.paqrap.logistics.pedidos.model.Pedido;
import com.paqrap.logistics.planificacion.model.EstadoRuta;
import com.paqrap.logistics.planificacion.model.ParadaRuta;
import com.paqrap.logistics.planificacion.model.Ruta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * La carga que muestra el mapa ("x/8 paquetes") son los paquetes aún no entregados de la ruta en
 * curso del vehículo. Moto del caso: capacidad 8 paquetes, 25 km/h, S/ 6.00/km.
 */
class CargaVehiculoTest {

    private static final TipoVehiculo MOTO = TipoVehiculo.builder()
            .codigo("TM").nombre("Moto").capacidadMaxima(8).velocidadPromedioKmH(25).costoPorKm(6.0).build();
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 10, 15, 8, 0);

    private SimulacionSession sesion;
    private UnidadTransporte tm01;

    @BeforeEach
    void preparar() {
        sesion = new SimulacionSession("test-carga");
        tm01 = UnidadTransporte.builder().codigo("TM01").tipo(MOTO).estadoOperativo(EstadoOperativo.EN_RUTA).build();
        sesion.getUnidades().add(tm01);
    }

    private ParadaRuta parada(String codigoPedido, int cantidad, int horasDesdeInicio) {
        Pedido p = Pedido.builder().codigo(codigoPedido).cantidadUnidades(cantidad).build();
        return ParadaRuta.builder().pedido(p).horaEstimadaLlegada(INICIO.plusHours(horasDesdeInicio)).build();
    }

    private Ruta ruta(long id, LocalDateTime generacion, ParadaRuta... paradas) {
        return Ruta.builder().id(id).fechaHoraGeneracion(generacion).unidadTransporte(tm01)
                .paradas(new ArrayList<>(List.of(paradas))).build();
    }

    @Test
    void alSalirLlevaTodoLoDeSuRutaYDescargaEnCadaEntrega() {
        ParadaRuta primera = parada("PED-1", 5, 1);
        ParadaRuta segunda = parada("PED-2", 3, 2);
        sesion.getRutas().add(ruta(1, INICIO, primera, segunda));

        sesion.actualizarCargas();
        assertEquals(8, tm01.getCargaActual(), "5 + 3 paquetes: la moto sale llena (8/8)");

        primera.setEntregada(true);
        sesion.actualizarCargas();
        assertEquals(3, tm01.getCargaActual());

        segunda.setEntregada(true);
        sesion.actualizarCargas();
        assertEquals(0, tm01.getCargaActual());
    }

    @Test
    void conRutasEncadenadasCuentaSoloLaQueEstaEnCurso() {
        sesion.getRutas().add(ruta(2, INICIO.plusHours(4), parada("PED-B", 6, 5)));
        sesion.getRutas().add(ruta(1, INICIO, parada("PED-A", 4, 1)));

        sesion.actualizarCargas();
        assertEquals(4, tm01.getCargaActual(), "la ruta de las 08:00 va primero; la de las 12:00 se carga después");
    }

    @Test
    void unaRutaCompletadaNoCuentaYElVehiculoQuedaVacio() {
        Ruta r = ruta(1, INICIO, parada("PED-1", 5, 1));
        r.setEstado(EstadoRuta.COMPLETADA);
        sesion.getRutas().add(r);
        tm01.setCargaActual(5);

        sesion.actualizarCargas();
        assertEquals(0, tm01.getCargaActual());
    }
}
