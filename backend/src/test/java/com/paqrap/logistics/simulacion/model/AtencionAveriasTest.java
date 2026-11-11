package com.paqrap.logistics.simulacion.model;

import com.paqrap.logistics.common.exception.BusinessException;
import com.paqrap.logistics.flota.model.Averia;
import com.paqrap.logistics.flota.model.EstadoOperativo;
import com.paqrap.logistics.flota.model.TipoAveria;
import com.paqrap.logistics.flota.model.TipoVehiculo;
import com.paqrap.logistics.flota.model.UnidadTransporte;
import com.paqrap.logistics.pedidos.model.EstadoPedido;
import com.paqrap.logistics.pedidos.model.Pedido;
import com.paqrap.logistics.planificacion.model.EstadoRuta;
import com.paqrap.logistics.planificacion.model.ParadaRuta;
import com.paqrap.logistics.planificacion.model.Ruta;
import com.paqrap.logistics.redvial.model.Ubicacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Averías (FAQ 3), con un auto TA01 que se avería el 15/10/2026 a las 10:00 en (30,14), camino a
 * entregar 10 paquetes en (37,14) a las 10:30. Central en (27,14).
 */
class AtencionAveriasTest {

    private static final TipoVehiculo AUTO = TipoVehiculo.builder()
            .codigo("TA").nombre("Auto").capacidadMaxima(24).velocidadPromedioKmH(40).costoPorKm(8.0).build();
    private static final LocalDateTime DIA = LocalDateTime.of(2026, 10, 15, 0, 0);
    private static final LocalDateTime EVENTO = DIA.withHour(10);
    private static final Ubicacion FALLA = new Ubicacion(30, 14);
    private static final Ubicacion CENTRAL = new Ubicacion(27, 14);

    private UnidadTransporte ta01;
    private Pedido pedido;
    private ParadaRuta parada;
    private Ruta ruta;

    @BeforeEach
    void preparar() {
        ta01 = UnidadTransporte.builder().codigo("TA01").tipo(AUTO)
                .estadoOperativo(EstadoOperativo.EN_RUTA).ubicacionActual(new Ubicacion(29, 14)).build();
        pedido = Pedido.builder().codigo("PED-1").cantidadUnidades(10).destino(new Ubicacion(37, 14))
                .estado(EstadoPedido.PLANIFICADO).rutaAsignadaId(1L).unidadAsignadaId("TA01").build();
        parada = ParadaRuta.builder().pedido(pedido).horaEstimadaLlegada(DIA.withHour(10).withMinute(30)).build();
        ruta = Ruta.builder().id(1L).fechaHoraGeneracion(DIA.withHour(9).withMinute(50)).unidadTransporte(ta01)
                .estado(EstadoRuta.EN_EJECUCION).paradas(new ArrayList<>(List.of(parada))).build();
    }

    private Averia averia(TipoAveria tipo) {
        Averia a = Averia.builder().codigo("AVE-1").tipo(tipo).fechaHoraEvento(EVENTO).ubicacionFalla(FALLA).build();
        AtencionAverias.iniciar(a, ta01);
        return a;
    }

    @Test
    void alAveriarseQuedaInmovilEnElPuntoDeLaFallaConSuCarga() {
        averia(TipoAveria.TIPO_2);
        assertEquals(EstadoOperativo.AVERIADA, ta01.getEstadoOperativo());
        assertEquals(FALLA, ta01.getUbicacionActual());
        assertEquals(10, ruta.cargaPendiente(), "sigue llevando sus 10 paquetes");
    }

    @Test
    void tipo1EsperaDosHorasYRetomaSuRutaConElHorarioCorrido() {
        Averia a = averia(TipoAveria.TIPO_1);
        assertEquals(DIA.withHour(12), a.getHoraReincorporacion());

        assertEquals(AtencionAverias.Cambio.NINGUNO, AtencionAverias.avanzar(a, ta01, ruta, DIA.withHour(11), CENTRAL, null));
        assertEquals(FALLA, ta01.getUbicacionActual());

        assertEquals(AtencionAverias.Cambio.REINCORPORADA, AtencionAverias.avanzar(a, ta01, ruta, DIA.withHour(12), CENTRAL, null));
        assertEquals(EstadoOperativo.EN_RUTA, ta01.getEstadoOperativo());
        assertEquals(DIA.withHour(12).withMinute(30), parada.getHoraEstimadaLlegada(), "10:30 + 2 h");
        assertTrue(a.isResuelta());
    }

    @Test
    void tipo2EsperaCuatroHorasSeTrasladaAlCentralYSuPedidoVuelveAPlanificarse() {
        Averia a = averia(TipoAveria.TIPO_2);
        // Avería a las 10:00 (turno 07-15): el turno siguiente es 15-23, se reincorpora a las 23:00
        assertEquals(DIA.withHour(23), a.getHoraReincorporacion());

        assertEquals(AtencionAverias.Cambio.NINGUNO,
                AtencionAverias.avanzar(a, ta01, ruta, DIA.withHour(13).withMinute(59), CENTRAL, null));
        assertEquals(FALLA, ta01.getUbicacionActual());

        List<ParadaRuta> liberadas = new ArrayList<>();
        assertEquals(AtencionAverias.Cambio.TRASLADADA_AL_CENTRAL,
                AtencionAverias.avanzar(a, ta01, ruta, DIA.withHour(14), CENTRAL, liberadas));
        assertEquals(CENTRAL, ta01.getUbicacionActual());
        assertEquals(EstadoOperativo.AVERIADA, ta01.getEstadoOperativo(), "sigue fuera de servicio hasta las 23:00");
        assertEquals(EstadoPedido.REGISTRADO, pedido.getEstado());
        assertNull(pedido.getUnidadAsignadaId());
        assertEquals(List.of(parada), liberadas);
        assertEquals(EstadoRuta.COMPLETADA, ruta.getEstado());
        assertEquals(0, ruta.cargaPendiente());

        assertEquals(AtencionAverias.Cambio.REINCORPORADA, AtencionAverias.avanzar(a, ta01, ruta, DIA.withHour(23), CENTRAL, null));
        assertEquals(EstadoOperativo.DISPONIBLE, ta01.getEstadoOperativo());
    }

    @Test
    void tipo2DeLaTardeSeReincorporaAlFinalDelTurnoDeLaNoche() {
        Averia a = Averia.builder().tipo(TipoAveria.TIPO_2).fechaHoraEvento(DIA.withHour(16)).build();
        // 16:00 (turno 15-23) -> turno siguiente 23-07 -> 07:00 del día siguiente
        assertEquals(DIA.plusDays(1).withHour(7), a.calcularReincorporacion(null));
    }

    @Test
    void tipo3SeTrasladaALas4HorasYVuelveDosDiasDespuesEnElTurnoDeLaTarde() {
        Averia a = averia(TipoAveria.TIPO_3);
        assertEquals(DIA.plusDays(2).withHour(15), a.getHoraReincorporacion(), "17/10 a las 15:00");

        AtencionAverias.avanzar(a, ta01, ruta, DIA.withHour(14), CENTRAL, new ArrayList<>());
        assertEquals(CENTRAL, ta01.getUbicacionActual());
        assertEquals(EstadoOperativo.AVERIADA, ta01.getEstadoOperativo());

        AtencionAverias.avanzar(a, ta01, ruta, DIA.plusDays(2).withHour(15), CENTRAL, null);
        assertEquals(EstadoOperativo.DISPONIBLE, ta01.getEstadoOperativo());
    }

    @Test
    void enUnaSesionNoSePuedeAveriarDosVecesLaMismaUnidad() {
        SimulacionSession sesion = new SimulacionSession("test-averia");
        sesion.getReloj().inicializar(EVENTO, 1.0);
        sesion.getUnidades().add(ta01);

        Averia a = sesion.registrarAveria("TA01", TipoAveria.TIPO_1, FALLA);
        assertEquals(EVENTO, a.getFechaHoraEvento());
        assertEquals(1, sesion.getAveriasOcurridas());
        assertThrows(BusinessException.class, () -> sesion.registrarAveria("TA01", TipoAveria.TIPO_2, FALLA));
        // El resumen para la API no arrastra referencias cíclicas
        assertEquals("TA01", AtencionAverias.resumen(a).get("idUnidad"));
    }
}
