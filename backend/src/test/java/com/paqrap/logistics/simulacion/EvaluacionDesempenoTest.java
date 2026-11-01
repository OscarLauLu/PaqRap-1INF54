package com.paqrap.logistics.simulacion;

import com.paqrap.logistics.almacen.model.Almacen;
import com.paqrap.logistics.almacen.model.AlmacenCentral;
import com.paqrap.logistics.almacen.model.AlmacenIntermedio;
import com.paqrap.logistics.almacen.repository.AlmacenRepository;
import com.paqrap.logistics.flota.model.EstadoOperativo;
import com.paqrap.logistics.flota.model.TipoVehiculo;
import com.paqrap.logistics.flota.model.Turno;
import com.paqrap.logistics.flota.model.UnidadTransporte;
import com.paqrap.logistics.pedidos.model.ConfiguracionSemaforo;
import com.paqrap.logistics.pedidos.model.EstadoPedido;
import com.paqrap.logistics.pedidos.model.NivelCriticidad;
import com.paqrap.logistics.pedidos.model.Pedido;
import com.paqrap.logistics.pedidos.model.TipoEntrega;
import com.paqrap.logistics.planificacion.algoritmo.AlgoritmoACO;
import com.paqrap.logistics.planificacion.algoritmo.AlgoritmoALNS;
import com.paqrap.logistics.planificacion.algoritmo.alns.AveriaDestroyOperator;
import com.paqrap.logistics.planificacion.algoritmo.alns.HolguraGreedyRepairOperator;
import com.paqrap.logistics.planificacion.algoritmo.alns.PlanSolution;
import com.paqrap.logistics.planificacion.model.ParadaRuta;
import com.paqrap.logistics.planificacion.model.Planificador;
import com.paqrap.logistics.planificacion.model.Ruta;
import com.paqrap.logistics.redvial.model.RedVial;
import com.paqrap.logistics.redvial.model.Ubicacion;
import com.paqrap.logistics.simulacion.model.ResultadoSimulacion;
import com.paqrap.logistics.simulacion.model.TipoEscenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Suite de pruebas automatizadas para la evaluación del desempeño del software (NFR y KPIs)
 * en la Simulación de Periodo (5 Días) y en la Operación Día a Día de PaqRap.
 */
public class EvaluacionDesempenoTest {

    private RedVial redVial;
    private List<UnidadTransporte> flota;
    private List<Almacen> almacenes;
    private AlmacenCentral central;
    private AlmacenIntermedio intermedio1;
    private AlmacenIntermedio intermedio2;

    @BeforeEach
    void setUp() {
        redVial = new RedVial();
        redVial.inicializarRed();

        // Tipos de vehículos según especificación PaqRap
        TipoVehiculo tipoAuto = TipoVehiculo.builder()
                .codigo("TA").nombre("Auto")
                .capacidadMaxima(24).velocidadPromedioKmH(40.0).costoPorKm(8.0)
                .build();
        TipoVehiculo tipoMoto = TipoVehiculo.builder()
                .codigo("TM").nombre("Moto")
                .capacidadMaxima(8).velocidadPromedioKmH(25.0).costoPorKm(6.0)
                .build();
        TipoVehiculo tipoBici = TipoVehiculo.builder()
                .codigo("TB").nombre("Bicicleta")
                .capacidadMaxima(4).velocidadPromedioKmH(12.0).costoPorKm(3.0)
                .build();

        flota = new ArrayList<>();
        // Flota mixta
        for (int i = 1; i <= 4; i++) {
            flota.add(UnidadTransporte.builder().codigo("TA0" + i).tipo(tipoAuto)
                    .estadoOperativo(EstadoOperativo.DISPONIBLE).ubicacionActual(new Ubicacion(27, 14)).build());
            flota.add(UnidadTransporte.builder().codigo("TM0" + i).tipo(tipoMoto)
                    .estadoOperativo(EstadoOperativo.DISPONIBLE).ubicacionActual(new Ubicacion(27, 14)).build());
            flota.add(UnidadTransporte.builder().codigo("TB0" + i).tipo(tipoBici)
                    .estadoOperativo(EstadoOperativo.DISPONIBLE).ubicacionActual(new Ubicacion(27, 14)).build());
        }

        // Almacenes: 1 Central permanente + 2 Intermedios con capacidad 1,000 unidades
        central = new AlmacenCentral("ALM-CEN", "Almacén Central", new Ubicacion(27, 14));
        intermedio1 = new AlmacenIntermedio("ALM-INT-01", "Almacén Norte", new Ubicacion(15, 35), 1000);
        intermedio2 = new AlmacenIntermedio("ALM-INT-02", "Almacén Sur", new Ubicacion(45, 15), 1000);

        almacenes = List.of(central, intermedio1, intermedio2);
    }

    @Test
    @DisplayName("EV-5D: Evaluación de desempeño en Simulación de Periodo (5 Días)")
    void testDesempenoSimulacionPeriodo5D() {
        LocalDateTime inicioSim = LocalDateTime.of(2026, 9, 1, 0, 0, 0);
        LocalDateTime finSim = inicioSim.plusDays(5); // 120 horas simuladas

        // 1. Generar conjunto representativo de pedidos distribuidos en los 5 días
        List<Pedido> pedidos5D = new ArrayList<>();
        for (int dia = 0; dia < 5; dia++) {
            LocalDateTime fechaDia = inicioSim.plusDays(dia);
            // Pedidos priorizados de 4h, 8h, 12h, 18h y regulares de 36h
            pedidos5D.add(crearPedido("PED-D" + dia + "-1", fechaDia.plusHours(8), TipoEntrega.PRIORIZADA_4H, 2, new Ubicacion(20, 20)));
            pedidos5D.add(crearPedido("PED-D" + dia + "-2", fechaDia.plusHours(10), TipoEntrega.PRIORIZADA_8H, 4, new Ubicacion(30, 25)));
            pedidos5D.add(crearPedido("PED-D" + dia + "-3", fechaDia.plusHours(12), TipoEntrega.PRIORIZADA_12H, 6, new Ubicacion(18, 30)));
            pedidos5D.add(crearPedido("PED-D" + dia + "-4", fechaDia.plusHours(14), TipoEntrega.REGULAR_36H, 10, new Ubicacion(35, 18)));
        }

        // 2. Medir tiempo de cómputo del algoritmo metaheurístico (NFR: <= 60 s por ciclo)
        AlgoritmoACO aco = new AlgoritmoACO();
        aco.getConfig().setIterations(15);
        aco.getConfig().setMinAnts(8);
        aco.getConfig().setMaxAnts(12);

        long tInicio = System.currentTimeMillis();
        List<Ruta> rutas5D = aco.construirSolucion(new ArrayList<>(pedidos5D), flota, redVial);
        long duracionMs = System.currentTimeMillis() - tInicio;

        // Validaciones NFR
        assertNotNull(rutas5D, "Las rutas generadas no deben ser nulas");
        assertFalse(rutas5D.isEmpty(), "Debe haberse construido al menos una ruta factible");
        assertTrue(duracionMs < 60000, "El tiempo de cómputo del ciclo debe ser <= 60 segundos (NFR). Duró: " + duracionMs + " ms");

        // 3. Evaluar KPIs logísticos del periodo
        int totalPedidos = pedidos5D.size();
        int entregasAuto = 0, entregasMoto = 0, entregasBici = 0;
        double costoTotal = 0.0;
        int pedidosEnPlazo = 0;

        for (Ruta r : rutas5D) {
            costoTotal += r.getCostoTotal();
            String tipo = (r.getUnidadTransporte() != null && r.getUnidadTransporte().getTipo() != null)
                    ? r.getUnidadTransporte().getTipo().getNombre().toUpperCase() : "";

            if (tipo.contains("AUTO")) entregasAuto += r.getParadas().size();
            else if (tipo.contains("MOTO")) entregasMoto += r.getParadas().size();
            else entregasBici += r.getParadas().size();

            for (ParadaRuta p : r.getParadas()) {
                if (p.getHoraEstimadaLlegada() != null && p.getPedido() != null
                        && p.getPedido().getPlazoLimiteEntrega() != null
                        && !p.getHoraEstimadaLlegada().isAfter(p.getPedido().getPlazoLimiteEntrega())) {
                    pedidosEnPlazo++;
                }
            }
        }

        double pctCumplimiento = (totalPedidos > 0) ? ((double) pedidosEnPlazo / totalPedidos) * 100.0 : 100.0;

        // 4. Evaluar recarga diaria de almacenes intermedios (5 recargas automáticas a las 23:59:59)
        for (int dia = 0; dia < 5; dia++) {
            LocalDateTime instanteRecarga = inicioSim.plusDays(dia).withHour(23).withMinute(59).withSecond(59);
            intermedio1.descontarStock(50, "RUT-TEST");
            assertTrue(intermedio1.getStockActual() < 1000);
            intermedio1.recargarDiario(instanteRecarga);
            assertEquals(1000, intermedio1.getStockActual(), "El almacén intermedio debe quedar recargado a 1,000 unidades a las 23:59:59");
        }

        // 5. Consolidación de ResultadoSimulacion (RF-73, RF-74)
        ResultadoSimulacion resultado5D = ResultadoSimulacion.builder()
                .codigo("SIM-5D-EVAL")
                .escenario(TipoEscenario.SIMULACION_5D.name())
                .fechaHoraInicio(inicioSim)
                .fechaHoraFin(finSim)
                .totalPedidos(totalPedidos)
                .pedidosEntregadosEnPlazo(pedidosEnPlazo)
                .porcentajeCumplimiento(pctCumplimiento)
                .costoTotalOperacion(costoTotal)
                .entregasAuto(entregasAuto)
                .entregasMoto(entregasMoto)
                .entregasBicicleta(entregasBici)
                .tiempoEjecucionRealSegundos(duracionMs / 1000)
                .build();

        assertNotNull(resultado5D.getCodigo());
        assertTrue(resultado5D.getPorcentajeCumplimiento() >= 90.0, "El cumplimiento en simulación 5D debe ser >= 90%");
        System.out.printf("[EV-5D OK] Cómputo: %d ms | Pedidos: %d | Cumplimiento: %.1f%% | Costo: S/ %.2f%n",
                duracionMs, totalPedidos, pctCumplimiento, costoTotal);
    }

    @Test
    @DisplayName("EV-DIA: Evaluación de desempeño y reactividad en Operación Día a Día")
    void testDesempenoOperacionDiaADia_ReactividadEnTiempoReal() {
        LocalDateTime ahora = LocalDateTime.of(2026, 10, 28, 8, 0, 0);

        // 1. Llegada dinámica de pedidos en caliente con diferentes prioridades
        ConfiguracionSemaforo semaforo = ConfiguracionSemaforo.builder()
                .umbralVerdeMin(Duration.ofHours(12))
                .umbralAmbarMin(Duration.ofHours(4))
                .build();

        Pedido pCritico = crearPedido("PED-URG", ahora, TipoEntrega.PRIORIZADA_4H, 3, new Ubicacion(26, 16));
        Pedido pMedio = crearPedido("PED-MED", ahora, TipoEntrega.PRIORIZADA_12H, 5, new Ubicacion(28, 20));
        Pedido pRegular = crearPedido("PED-REG", ahora, TipoEntrega.REGULAR_36H, 8, new Ubicacion(32, 14));

        // Fijar plazos explícitos para obtener holguras deterministas respecto a 'ahora':
        // pCritico: holgura = 2h  -> ROJO  (< 4h)
        // pMedio:   holgura = 8h  -> AMBAR (>= 4h y < 12h)
        // pRegular: holgura = 24h -> VERDE (>= 12h)
        pCritico.setPlazoLimiteEntrega(ahora.plusHours(2));
        pMedio.setPlazoLimiteEntrega(ahora.plusHours(8));
        pRegular.setPlazoLimiteEntrega(ahora.plusHours(24));

        // Evaluar clasificación semafórica dinámica (RF-03, RF-53)
        pCritico.evaluarCriticidad(semaforo, ahora);
        pMedio.evaluarCriticidad(semaforo, ahora);
        pRegular.evaluarCriticidad(semaforo, ahora);

        assertEquals(NivelCriticidad.ROJO, pCritico.getNivelCriticidad(), "Pedido con holgura < 4h debe ser clasificado en ROJO");
        assertEquals(NivelCriticidad.AMBAR, pMedio.getNivelCriticidad(), "Pedido con holgura entre 4h y 12h debe ser clasificado en AMBAR");
        assertEquals(NivelCriticidad.VERDE, pRegular.getNivelCriticidad(), "Pedido con holgura >= 12h debe ser clasificado en VERDE");

        // 2. Medir tiempo de respuesta de planificación en tiempo real (NFR: reactividad instantánea)
        AlmacenRepository mockAlmacenRepo = Mockito.mock(AlmacenRepository.class);
        when(mockAlmacenRepo.findAll()).thenReturn(almacenes);

        Planificador planificador = new Planificador(new AlgoritmoACO(), redVial, null, null, mockAlmacenRepo, null);

        long tIniOrden = System.currentTimeMillis();
        List<Pedido> ordenados = planificador.ordenarPorHolgura(List.of(pRegular, pMedio, pCritico), ahora);
        long tFinOrden = System.currentTimeMillis() - tIniOrden;

        assertTrue(tFinOrden < 100, "El ordenamiento por holgura debe ser inmediato (< 100 ms)");
        assertEquals("PED-URG", ordenados.get(0).getCodigo(), "El pedido más crítico (ROJO) debe tener prioridad 1");

        // 3. Medir reactividad ante contingencias en caliente (Avería Mecánica en ruta, RF-14 a RF-16)
        UnidadTransporte vehiculoAveriado = flota.get(0); // TA01
        Ruta rutaActiva = Ruta.builder()
                .codigo("RUT-ACTIVA-01")
                .unidadTransporte(vehiculoAveriado)
                .almacenOrigen(central)
                .paradas(new ArrayList<>())
                .build();
        rutaActiva.agregarParada(ParadaRuta.builder().pedido(pCritico).tiempoServicioMin(60).build());

        PlanSolution solucionViva = new PlanSolution();
        solucionViva.getRutas().add(rutaActiva);

        long tInicioReplan = System.currentTimeMillis();

        // Inyección de avería
        vehiculoAveriado.cambiarEstado(EstadoOperativo.AVERIADA);
        vehiculoAveriado.setActivo(false);

        AveriaDestroyOperator desalojador = new AveriaDestroyOperator(flota);
        List<Pedido> paquetesRescatados = desalojador.destruir(solucionViva, 0.20);
        assertEquals(1, paquetesRescatados.size(), "Debe desalojar el pedido de la unidad averiada");

        HolguraGreedyRepairOperator reasignador = new HolguraGreedyRepairOperator(redVial, almacenes, flota);
        reasignador.reparar(solucionViva, paquetesRescatados);

        long tReplanMs = System.currentTimeMillis() - tInicioReplan;

        assertTrue(tReplanMs < 5000, "La replanificación reactiva en día a día debe responder en < 5 segundos. Duró: " + tReplanMs + " ms");
        boolean pedidoReasignadoASano = solucionViva.getRutas().stream()
                .filter(r -> !r.getParadas().isEmpty())
                .allMatch(r -> r.getUnidadTransporte().getEstadoOperativo() == EstadoOperativo.DISPONIBLE);
        assertTrue(pedidoReasignadoASano, "El pedido de la unidad averiada debe haber sido reasignado a una unidad sana");

        System.out.printf("[EV-DIA OK] Reactividad de Replanificación por Avería: %d ms | Pedido rescatado con éxito.%n", tReplanMs);
    }

    @Test
    @DisplayName("EV-COMP: Comparación formal de desempeño entre corridas de simulación (RF-75)")
    void testComparacionDesempenoEntreCorridas() {
        ResultadoSimulacion corridaACO = ResultadoSimulacion.builder()
                .codigo("SIM-ACO-001")
                .escenario("SIMULACION_5D")
                .porcentajeCumplimiento(98.5)
                .costoTotalOperacion(4250.0)
                .totalPedidos(100)
                .pedidosEntregadosEnPlazo(98)
                .build();

        ResultadoSimulacion corridaALNS = ResultadoSimulacion.builder()
                .codigo("SIM-ALNS-001")
                .escenario("SIMULACION_5D")
                .porcentajeCumplimiento(95.0)
                .costoTotalOperacion(4600.0)
                .totalPedidos(100)
                .pedidosEntregadosEnPlazo(95)
                .build();

        Map<String, Double> comparativa = corridaACO.compararCon(corridaALNS);

        assertNotNull(comparativa);
        assertEquals(3.5, comparativa.get("diffCumplimientoPct"), 0.01, "ACO debe tener +3.5% de cumplimiento");
        assertEquals(-350.0, comparativa.get("diffCostoTotal"), 0.01, "ACO debe tener un ahorro de S/ 350.00");
        assertEquals(3.0, comparativa.get("diffEntregadosEnPlazo"), 0.01, "ACO debe haber entregado 3 pedidos más en plazo");

        System.out.printf("[EV-COMP OK] Diff Cumplimiento: +%.1f%% | Diff Costo: S/ %.2f%n",
                comparativa.get("diffCumplimientoPct"), comparativa.get("diffCostoTotal"));
    }

    @Test
    @DisplayName("EV-RUNNER: Ejecución completa del runner CLI de evaluación de desempeño")
    void testRunnerEjecutaSinErrores() {
        boolean ok = com.paqrap.logistics.simulacion.model.EvaluadorDesempenoRunner.ejecutarEvaluacionCompleta();
        assertTrue(ok, "El runner de evaluación de desempeño debe completar 100% exitoso");
    }

    private Pedido crearPedido(String codigo, LocalDateTime fechaRegistro, TipoEntrega tipo, int unidades, Ubicacion destino) {
        return Pedido.builder()
                .codigo(codigo)
                .fechaHoraRegistro(fechaRegistro)
                .plazoLimiteEntrega(fechaRegistro.plusHours(tipo.getHorasPlazo()))
                .tipoEntrega(tipo)
                .cantidadUnidades(unidades)
                .destino(destino)
                .estado(EstadoPedido.REGISTRADO)
                .build();
    }
}
