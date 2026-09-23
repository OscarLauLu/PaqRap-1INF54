package com.paqrap.logistics.planificacion.algoritmo;

import com.paqrap.logistics.almacen.model.Almacen;
import com.paqrap.logistics.almacen.model.AlmacenCentral;
import com.paqrap.logistics.almacen.repository.AlmacenRepository;
import com.paqrap.logistics.flota.model.EstadoOperativo;
import com.paqrap.logistics.flota.model.TipoVehiculo;
import com.paqrap.logistics.flota.model.UnidadTransporte;
import com.paqrap.logistics.pedidos.model.EstadoPedido;
import com.paqrap.logistics.pedidos.model.Pedido;
import com.paqrap.logistics.pedidos.model.TipoEntrega;
import com.paqrap.logistics.planificacion.model.Ruta;
import com.paqrap.logistics.redvial.model.RedVial;
import com.paqrap.logistics.redvial.model.Ubicacion;
import com.paqrap.logistics.planificacion.algoritmo.alns.AveriaDestroyOperator;
import com.paqrap.logistics.planificacion.algoritmo.alns.HolguraGreedyRepairOperator;
import com.paqrap.logistics.planificacion.algoritmo.alns.PlanSolution;
import com.paqrap.logistics.planificacion.model.ParadaRuta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class AlgoritmosTest {

    private RedVial redVial;
    private List<UnidadTransporte> flota;
    private List<Pedido> pedidos;
    private List<Almacen> almacenes;
    private AlmacenRepository almacenRepository;

    @BeforeEach
    void setUp() {
        redVial = new RedVial();
        redVial.inicializarRed();

        TipoVehiculo tipoAuto = TipoVehiculo.builder()
                .codigo("TA")
                .nombre("Auto")
                .capacidadMaxima(24)
                .velocidadPromedioKmH(20.0)
                .costoPorKm(8.0)
                .build();

        flota = new ArrayList<>();
        flota.add(UnidadTransporte.builder()
                .codigo("TA01")
                .tipo(tipoAuto)
                .estadoOperativo(EstadoOperativo.DISPONIBLE)
                .ubicacionActual(new Ubicacion(27, 14))
                .dadaDeBaja(false)
                .build());

        flota.add(UnidadTransporte.builder()
                .codigo("TA02")
                .tipo(tipoAuto)
                .estadoOperativo(EstadoOperativo.DISPONIBLE)
                .ubicacionActual(new Ubicacion(27, 14))
                .dadaDeBaja(false)
                .build());

        LocalDateTime ahora = LocalDateTime.now();
        pedidos = new ArrayList<>();
        pedidos.add(Pedido.builder()
                .id(101L)
                .codigo("PED-001")
                .cantidadUnidades(5)
                .fechaHoraRegistro(ahora)
                .plazoLimiteEntrega(ahora.plusHours(6))
                .estado(EstadoPedido.REGISTRADO)
                .tipoEntrega(TipoEntrega.REGULAR_36H)
                .destino(new Ubicacion(12, 14))
                .build());

        pedidos.add(Pedido.builder()
                .id(102L)
                .codigo("PED-002")
                .cantidadUnidades(8)
                .fechaHoraRegistro(ahora)
                .plazoLimiteEntrega(ahora.plusHours(4))
                .estado(EstadoPedido.REGISTRADO)
                .tipoEntrega(TipoEntrega.PRIORIZADA_4H)
                .destino(new Ubicacion(15, 12))
                .build());

        pedidos.add(Pedido.builder()
                .id(103L)
                .codigo("PED-003")
                .cantidadUnidades(6)
                .fechaHoraRegistro(ahora)
                .plazoLimiteEntrega(ahora.plusHours(8))
                .estado(EstadoPedido.REGISTRADO)
                .tipoEntrega(TipoEntrega.REGULAR_36H)
                .destino(new Ubicacion(18, 16))
                .build());

        almacenes = new ArrayList<>();
        almacenes.add(new AlmacenCentral("ALM-CEN-01", "Almacén Central", new Ubicacion(27, 14)));

        almacenRepository = Mockito.mock(AlmacenRepository.class);
        when(almacenRepository.findAll()).thenReturn(almacenes);
    }

    @Test
    void testAlgoritmoACO_ConstruyeSolucionValida() {
        AlgoritmoACO aco = new AlgoritmoACO();
        aco.getConfig().setIterations(10);
        aco.getConfig().setMinAnts(5);
        aco.getConfig().setMaxAnts(10);

        List<Ruta> rutas = aco.construirSolucion(pedidos, flota, redVial);

        assertNotNull(rutas);
        assertFalse(rutas.isEmpty(), "ACO debe generar al menos una ruta");
        assertTrue(aco.obtenerCostoSolucion() > 0.0, "El costo de la solución debe ser positivo");

        for (Ruta r : rutas) {
            assertFalse(r.capacidadExcedida(), "Ninguna ruta debe exceder la capacidad máxima");
            assertTrue(r.cumplePlazos(), "Todas las paradas deben cumplir los plazos comprometidos");
        }
    }

    @Test
    void testAlgoritmoALNS_ConstruyeSolucionValida() {
        AlgoritmoALNS alns = new AlgoritmoALNS(almacenRepository);
        alns.setLimiteMillis(800);

        List<Ruta> rutas = alns.construirSolucion(pedidos, flota, redVial);

        assertNotNull(rutas);
        assertFalse(rutas.isEmpty(), "ALNS debe generar al menos una ruta");
        assertTrue(alns.obtenerCostoSolucion() > 0.0, "El costo de la solución debe ser positivo");

        for (Ruta r : rutas) {
            assertFalse(r.capacidadExcedida(), "Ninguna ruta debe exceder la capacidad máxima");
            assertTrue(r.cumplePlazos(), "Todas las paradas deben cumplir los plazos");
        }
    }

    @Test
    void testContingenciaAveria_LiberaYReasignaPedidos() {
        UnidadTransporte u1 = flota.get(0); // TA01
        UnidadTransporte u2 = flota.get(1); // TA02

        Ruta rutaInicial = Ruta.builder()
                .codigo("RUT-TEST-01")
                .unidadTransporte(u1)
                .almacenOrigen(almacenes.get(0))
                .fechaHoraGeneracion(LocalDateTime.now())
                .paradas(new ArrayList<>())
                .build();

        for (Pedido p : pedidos) {
            rutaInicial.agregarParada(ParadaRuta.builder().pedido(p).tiempoServicioMin(60).build());
        }

        PlanSolution plan = new PlanSolution();
        plan.getRutas().add(rutaInicial);

        // 1. Simular avería mecánica en TA01 (RF-14)
        u1.cambiarEstado(EstadoOperativo.AVERIADA);
        u1.setActivo(false);

        // 2. Ejecutar AveriaDestroyOperator: debe desalojar los paquetes de la unidad averiada
        AveriaDestroyOperator destroyAveria = new AveriaDestroyOperator(flota);
        List<Pedido> liberados = destroyAveria.destruir(plan, 0.20);

        assertFalse(liberados.isEmpty(), "AveriaDestroyOperator debe liberar los pedidos de la unidad averiada");
        assertEquals(3, liberados.size(), "Debe liberar los 3 pedidos de TA01");
        assertTrue(rutaInicial.getParadas().isEmpty(), "La ruta de la unidad averiada debe quedar vacía");

        // 3. Ejecutar HolguraGreedyRepairOperator para reasignar a VEH-002 (RF-15, RF-16)
        HolguraGreedyRepairOperator repair = new HolguraGreedyRepairOperator(redVial, almacenes, flota);
        repair.reparar(plan, liberados);

        // Verificar que los pedidos fueron reinsertados en unidades disponibles
        boolean asignadosEnVehiculoSano = plan.getRutas().stream()
                .filter(r -> !r.getParadas().isEmpty())
                .allMatch(r -> r.getUnidadTransporte() != null && r.getUnidadTransporte().getEstadoOperativo() == EstadoOperativo.DISPONIBLE);

        assertTrue(asignadosEnVehiculoSano, "Todos los pedidos deben haber sido rescatados y reasignados a vehículos operativos");
    }
}
