package com.paqrap.logistics.planificacion.model;

import com.paqrap.logistics.flota.model.EstadoOperativo;
import com.paqrap.logistics.flota.model.TipoVehiculo;
import com.paqrap.logistics.flota.model.UnidadTransporte;
import com.paqrap.logistics.pedidos.model.Pedido;
import com.paqrap.logistics.planificacion.algoritmo.AlgoritmoRuteo;
import com.paqrap.logistics.redvial.model.RedVial;
import com.paqrap.logistics.redvial.model.Ubicacion;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Un vehículo no sale en el pasado: el ACO fecha sus rutas desde el pedido pendiente más antiguo
 * (08:08), pero se planifican a las 09:00. Las rutas deben salir a las 09:00 conservando los tiempos
 * entre paradas; de lo contrario el motor da por entregadas al instante las paradas "ya vencidas".
 */
class PlanificadorSalidaTest {

    private static final TipoVehiculo MOTO = TipoVehiculo.builder()
            .codigo("TM").nombre("Moto").capacidadMaxima(8).velocidadPromedioKmH(25).costoPorKm(6).build();
    private static final LocalDateTime ACO_BASE = LocalDateTime.of(2026, 10, 15, 8, 8);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 15, 9, 0);

    private static Ruta rutaDelAlgoritmo(String unidad, LocalDateTime salida, int minutosHastaParada) {
        ParadaRuta parada = ParadaRuta.builder()
                .pedido(Pedido.builder().codigo("PED-" + unidad).cantidadUnidades(5).destino(new Ubicacion(34, 13)).build())
                .horaEstimadaLlegada(salida.plusMinutes(minutosHastaParada))
                .build();
        return Ruta.builder()
                .fechaHoraGeneracion(salida)
                .unidadTransporte(UnidadTransporte.builder().codigo(unidad).tipo(MOTO).estadoOperativo(EstadoOperativo.DISPONIBLE).build())
                .paradas(new ArrayList<>(List.of(parada)))
                .build();
    }

    private Planificador conAlgoritmo(List<Ruta> queDevuelve) {
        AlgoritmoRuteo algoritmo = new AlgoritmoRuteo() {
            @Override
            public List<Ruta> construirSolucion(List<Pedido> pedidos, List<UnidadTransporte> flota, RedVial red) {
                return queDevuelve;
            }

            @Override
            public void configurarParametros(java.util.Map<String, Double> params) {
            }

            @Override
            public String obtenerNombre() {
                return "falso";
            }

            @Override
            public double obtenerCostoSolucion() {
                return 0;
            }
        };
        return new Planificador(algoritmo, null, null, null, null, null);
    }

    @Test
    void laPrimeraRutaSaleEnElInstanteDePlanificacionYSeConservanLosTiemposEntreParadas() {
        // TM01 llega 20 min después de salir; TM05 a los 58 min; la segunda ruta de TM01 sale a las 09:08 del algoritmo
        Ruta tm01 = rutaDelAlgoritmo("TM01", ACO_BASE, 20);
        Ruta tm05 = rutaDelAlgoritmo("TM05", ACO_BASE, 58);
        Ruta tm01Segunda = rutaDelAlgoritmo("TM01", ACO_BASE.plusMinutes(80), 20);
        Planificador planificador = conAlgoritmo(List.of(tm01, tm05, tm01Segunda));

        List<Ruta> rutas = planificador.construirSolucionRespetandoMantenimiento(List.of(), List.of(), AHORA);

        assertEquals(AHORA, tm01.getFechaHoraGeneracion(), "sale a las 09:00 y no a las 08:08");
        assertEquals(AHORA.plusMinutes(20), tm01.getParadas().get(0).getHoraEstimadaLlegada());
        assertEquals(AHORA.plusMinutes(58), tm05.getParadas().get(0).getHoraEstimadaLlegada());
        assertEquals(AHORA.plusMinutes(80), tm01Segunda.getFechaHoraGeneracion(), "la cadena de rutas se corre igual");
        assertTrue(rutas.stream().allMatch(r -> !r.getFechaHoraGeneracion().isBefore(AHORA)));
        // Ninguna parada nace con su hora ya vencida: el motor no las entrega al instante
        assertTrue(rutas.stream().flatMap(r -> r.getParadas().stream())
                .allMatch(p -> p.getHoraEstimadaLlegada().isAfter(AHORA)));
    }

    @Test
    void siElAlgoritmoYaSalePosteriorAlInstanteNoSeAdelanta() {
        Ruta futura = rutaDelAlgoritmo("TM01", AHORA.plusMinutes(10), 20);
        Planificador planificador = conAlgoritmo(List.of(futura));

        planificador.construirSolucionRespetandoMantenimiento(List.of(), List.of(), AHORA);

        assertEquals(AHORA.plusMinutes(10), futura.getFechaHoraGeneracion());
        assertEquals(AHORA.plusMinutes(30), futura.getParadas().get(0).getHoraEstimadaLlegada());
    }
}
