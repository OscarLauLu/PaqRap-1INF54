package com.paqrap.logistics.planificacion.model;

import com.paqrap.logistics.almacen.model.Almacen;
import com.paqrap.logistics.almacen.model.AlmacenCentral;
import com.paqrap.logistics.flota.model.Averia;
import com.paqrap.logistics.flota.model.EstadoOperativo;
import com.paqrap.logistics.flota.model.TipoAveria;
import com.paqrap.logistics.flota.model.TipoVehiculo;
import com.paqrap.logistics.flota.model.UnidadTransporte;
import com.paqrap.logistics.pedidos.model.EstadoPedido;
import com.paqrap.logistics.pedidos.model.Pedido;
import com.paqrap.logistics.redvial.model.Ubicacion;
import com.paqrap.logistics.simulacion.model.AtencionAverias;
import com.paqrap.logistics.simulacion.model.SimulacionSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Rescate por trasvase (30 min en el punto de la avería). Flota del caso: auto 24 paquetes a 40 km/h,
 * moto 8 a 25 km/h, bicicleta 4 a 12 km/h. TA01 se avería el 15/10/2026 a las 10:00 en (40,14)
 * llevando 10 paquetes para (45,20) y 8 para (50,14).
 */
class PlanificadorTrasvaseTest {

    private static final TipoVehiculo AUTO = tipo("TA", "Auto", 24, 40, 8);
    private static final TipoVehiculo MOTO = tipo("TM", "Moto", 8, 25, 6);
    private static final TipoVehiculo BICI = tipo("TB", "Bicicleta", 4, 12, 3);
    private static final LocalDateTime DIA = LocalDateTime.of(2026, 10, 15, 0, 0);
    private static final LocalDateTime EVENTO = DIA.withHour(10);
    private static final Ubicacion FALLA = new Ubicacion(40, 14);
    private static final List<Almacen> ALMACENES = List.of(new AlmacenCentral("ALM-CEN-01", "Central", new Ubicacion(27, 14)));

    private UnidadTransporte ta01;
    private Ruta rutaAveriada;
    private ParadaRuta paraDiez;
    private ParadaRuta paraOcho;
    private final List<UnidadTransporte> flota = new ArrayList<>();
    private final List<Ruta> rutas = new ArrayList<>();

    private static TipoVehiculo tipo(String c, String n, int cap, double vel, double costo) {
        return TipoVehiculo.builder().codigo(c).nombre(n).capacidadMaxima(cap).velocidadPromedioKmH(vel).costoPorKm(costo).build();
    }

    private static UnidadTransporte unidad(String codigo, TipoVehiculo t, EstadoOperativo e, int x, int y) {
        return UnidadTransporte.builder().codigo(codigo).tipo(t).estadoOperativo(e).ubicacionActual(new Ubicacion(x, y)).build();
    }

    private static ParadaRuta parada(String codigo, int cantidad, int x, int y, LocalDateTime llegada, LocalDateTime plazo) {
        Pedido p = Pedido.builder().codigo(codigo).cantidadUnidades(cantidad).destino(new Ubicacion(x, y))
                .estado(EstadoPedido.PLANIFICADO).plazoLimiteEntrega(plazo).build();
        return ParadaRuta.builder().pedido(p).horaEstimadaLlegada(llegada).build();
    }

    @BeforeEach
    void preparar() {
        ta01 = unidad("TA01", AUTO, EstadoOperativo.EN_RUTA, 39, 14);
        paraDiez = parada("PED-10", 10, 45, 20, DIA.withHour(10).withMinute(30), DIA.withHour(20));
        paraOcho = parada("PED-8", 8, 50, 14, DIA.withHour(12), DIA.withHour(18));
        rutaAveriada = Ruta.builder().id(1L).codigo("R1").unidadTransporte(ta01).fechaHoraGeneracion(DIA.withHour(9))
                .estado(EstadoRuta.EN_EJECUCION).paradas(new ArrayList<>(List.of(paraDiez, paraOcho))).build();
        flota.add(ta01);
        rutas.add(rutaAveriada);
    }

    private Averia averia(TipoAveria tipo) {
        Averia a = Averia.builder().codigo("AVE-1").tipo(tipo).fechaHoraEvento(EVENTO).ubicacionFalla(FALLA).build();
        AtencionAverias.iniciar(a, ta01);
        return a;
    }

    private Ruta planificar(Averia a, LocalDateTime instante) {
        Map<String, Ruta> enCurso = Ruta.rutaEnCursoPorUnidad(rutas);
        return PlanificadorTrasvase.planificar(a, rutaAveriada, flota, enCurso, instante, ALMACENES, "RES-1");
    }

    @Test
    void vaLaUnidadQueLlegaPrimeroAunqueEsteRegresandoYSeLlevaTodaLaCarga() {
        flota.add(unidad("TA02", AUTO, EstadoOperativo.DISPONIBLE, 27, 14));    // 13 km: llega 10:19:30
        flota.add(unidad("TB01", BICI, EstadoOperativo.DISPONIBLE, 39, 14));    // 1 km, pero no le cabe ningún pedido
        UnidadTransporte ta04 = unidad("TA04", AUTO, EstadoOperativo.EN_RUTA, 45, 14);  // regresa: 5 km, llega 10:07:30
        flota.add(ta04);
        Ruta regresoTa04 = Ruta.builder().id(2L).codigo("R2").unidadTransporte(ta04).fechaHoraGeneracion(DIA.withHour(8))
                .estado(EstadoRuta.EN_EJECUCION).paradas(new ArrayList<>()).build();
        rutas.add(regresoTa04);

        Ruta rescate = planificar(averia(TipoAveria.TIPO_2), EVENTO);

        assertNotNull(rescate);
        assertSame(ta04, rescate.getUnidadTransporte());
        assertEquals(DIA.withHour(10).withMinute(7).withSecond(30), rescate.getRescate().getLlegada());
        assertEquals(DIA.withHour(10).withMinute(37).withSecond(30), rescate.getRescate().getFin(), "trasvase de 30 min");
        assertEquals(18, rescate.getRescate().getCantidadProductos());
        assertEquals(EstadoRuta.COMPLETADA, regresoTa04.getEstado(), "se desvía: deja su regreso");
        assertTrue(rutaAveriada.getParadas().isEmpty(), "ya no lleva nada pendiente la averiada");
        // Desde (40,14) va primero a lo más cercano: (50,14) a 10 km (no (45,20), a 11) -> 10:37:30 + 15 min
        assertEquals("PED-8", rescate.getParadas().get(0).getPedido().getCodigo());
        assertEquals(DIA.withHour(10).withMinute(52).withSecond(30), rescate.getParadas().get(0).getHoraEstimadaLlegada());
        assertEquals("TA04", paraDiez.getPedido().getUnidadAsignadaId());
    }

    @Test
    void siSoloCabeParteSeLlevaLoQueCabePorOrdenDePlazoYElRestoSeQuedaEnLaAveriada() {
        flota.add(unidad("TM01", MOTO, EstadoOperativo.DISPONIBLE, 27, 14));   // capacidad 8

        Ruta rescate = planificar(averia(TipoAveria.TIPO_2), EVENTO);

        assertNotNull(rescate);
        assertEquals(List.of(paraOcho), rescate.getParadas(), "el de 8 vence primero (18:00) y le cabe");
        assertEquals(List.of(paraDiez), rutaAveriada.getParadas(), "los 10 paquetes siguen en TA01");
    }

    @Test
    void tipo2SinNadieQueTermineElTrasvaseAntesDeLas4HorasNoSeRescata() {
        flota.add(unidad("TA02", AUTO, EstadoOperativo.DISPONIBLE, 27, 14));   // tarda 19.5 min + 30 de trasvase
        // A las 13:20 terminaría el trasvase a las 14:09:30, después de las 14:00 en que TA01 deja el lugar
        assertNull(planificar(averia(TipoAveria.TIPO_2), DIA.withHour(13).withMinute(20)));
    }

    @Test
    void tipo1SoloSeRescataSiEsperarLasDosHorasHariaLlegarTardeAlgunPedido() {
        flota.add(unidad("TA02", AUTO, EstadoOperativo.DISPONIBLE, 27, 14));
        Averia a = averia(TipoAveria.TIPO_1);
        // Esperando 2 h: PED-10 llegaría 12:30 (plazo 20:00) y PED-8 14:00 (plazo 18:00): nadie llega tarde
        assertNull(planificar(a, EVENTO));

        paraOcho.getPedido().setPlazoLimiteEntrega(DIA.withHour(13));   // con la espera llegaría 14:00, tarde
        assertNotNull(planificar(a, EVENTO));
    }

    @Test
    void mientrasDuraElTrasvaseLaCargaSigueEnLaAveriadaYDespuesPasaALaQueRescata() {
        UnidadTransporte ta02 = unidad("TA02", AUTO, EstadoOperativo.DISPONIBLE, 27, 14);
        Averia a = averia(TipoAveria.TIPO_2);
        SimulacionSession sesion = new SimulacionSession("test-trasvase");
        sesion.getUnidades().addAll(List.of(ta01, ta02));
        sesion.getRutas().add(rutaAveriada);
        flota.add(ta02);
        Ruta rescate = planificar(a, EVENTO);
        sesion.getRutas().add(rescate);

        sesion.getReloj().inicializar(DIA.withHour(10).withMinute(30), 1.0);   // trasvase hasta 10:49:30
        sesion.actualizarCargas();
        assertEquals(18, ta01.getCargaActual());
        assertEquals(0, ta02.getCargaActual());

        sesion.getReloj().inicializar(DIA.withHour(10).withMinute(50), 1.0);
        sesion.actualizarCargas();
        assertEquals(0, ta01.getCargaActual());
        assertEquals(18, ta02.getCargaActual());
    }

    @Test
    void laUnidadQueRescataSeVeYendoAlPuntoDeLaAveriaYAllíEsperaElTrasvase() {
        UnidadTransporte ta02 = unidad("TA02", AUTO, EstadoOperativo.DISPONIBLE, 27, 14);
        flota.add(ta02);
        Ruta rescate = planificar(averia(TipoAveria.TIPO_2), EVENTO);   // llega 10:19:30 a (40,14)

        assertTrue(rescate.avanzarTramoRescate(EVENTO.plusSeconds(585)));   // mitad del camino: 6.5 km
        assertEquals(new Ubicacion(34, 14), ta02.getUbicacionActual());
        assertTrue(rescate.avanzarTramoRescate(DIA.withHour(10).withMinute(30)));
        assertEquals(FALLA, ta02.getUbicacionActual(), "trasvasando en el punto de la avería");
        assertFalse(rescate.avanzarTramoRescate(DIA.withHour(10).withMinute(50)), "terminó el trasvase");
        assertEquals(FALLA, rescate.origenEntregas());
    }
}
