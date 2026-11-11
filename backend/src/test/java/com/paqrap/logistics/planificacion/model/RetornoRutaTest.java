package com.paqrap.logistics.planificacion.model;

import com.paqrap.logistics.almacen.model.Almacen;
import com.paqrap.logistics.almacen.model.AlmacenCentral;
import com.paqrap.logistics.almacen.model.AlmacenIntermedio;
import com.paqrap.logistics.flota.model.EstadoOperativo;
import com.paqrap.logistics.flota.model.TipoVehiculo;
import com.paqrap.logistics.flota.model.UnidadTransporte;
import com.paqrap.logistics.pedidos.model.Pedido;
import com.paqrap.logistics.redvial.model.Ubicacion;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Al terminar las entregas el vehículo vuelve al almacén con stock más cercano (FAQ 10) y se le ve
 * viajando. Almacenes oficiales: central (27,14), Nor-Oeste (12,38), Este (57,27). Auto: 40 km/h.
 */
class RetornoRutaTest {

    private static final TipoVehiculo AUTO = TipoVehiculo.builder()
            .codigo("TA").nombre("Auto").capacidadMaxima(24).velocidadPromedioKmH(40).costoPorKm(8.0).build();
    private static final LocalDateTime SALIDA = LocalDateTime.of(2026, 10, 15, 8, 0);

    private final Almacen central = new AlmacenCentral("ALM-CEN-01", "Central", new Ubicacion(27, 14));
    private final AlmacenIntermedio norOeste = new AlmacenIntermedio("ALM-INT-01", "Nor-Oeste", new Ubicacion(12, 38), 1000);
    private final AlmacenIntermedio este = new AlmacenIntermedio("ALM-INT-02", "Este", new Ubicacion(57, 27), 1000);
    private final List<Almacen> almacenes = List.of(central, norOeste, este);

    /** Ruta de TA01: una parada en "destino" a la que llega a las 08:15 y acondiciona 1 h (hasta 09:15). */
    private Ruta rutaEntregada(Ubicacion destino) {
        UnidadTransporte ta01 = UnidadTransporte.builder().codigo("TA01").tipo(AUTO)
                .estadoOperativo(EstadoOperativo.EN_RUTA).ubicacionActual(destino).build();
        ParadaRuta parada = ParadaRuta.builder()
                .pedido(Pedido.builder().codigo("PED-1").cantidadUnidades(10).destino(destino).build())
                .horaEstimadaLlegada(SALIDA.plusMinutes(15)).entregada(true).build();
        return Ruta.builder().fechaHoraGeneracion(SALIDA).unidadTransporte(ta01).almacenOrigen(central)
                .paradas(new ArrayList<>(List.of(parada))).build();
    }

    @Test
    void vuelveAlCentralQueEsElMasCercanoYTardaLoQueDiceSuVelocidad() {
        Ruta r = rutaEntregada(new Ubicacion(37, 14));   // 10 km del central, 33 km del Este

        assertSame(central, Ruta.almacenDeRetorno(r.puntoFinalEntregas(), almacenes));
        // Sale a las 09:15 (tras 1 h de acondicionamiento); 10 km a 40 km/h = 15 min
        assertEquals(LocalDateTime.of(2026, 10, 15, 9, 30), r.horaLlegadaAlmacen(central));
    }

    @Test
    void mientrasRegresaSeVeViajandoYAlLlegarQuedaDisponibleEnElAlmacen() {
        Ruta r = rutaEntregada(new Ubicacion(37, 14));
        UnidadTransporte ta01 = r.getUnidadTransporte();

        assertFalse(r.avanzarRetorno(LocalDateTime.of(2026, 10, 15, 9, 22, 30), almacenes));
        assertEquals(new Ubicacion(32, 14), ta01.getUbicacionActual(), "a mitad del regreso");
        assertEquals(EstadoOperativo.EN_RUTA, ta01.getEstadoOperativo());
        assertNotEquals(EstadoRuta.COMPLETADA, r.getEstado());

        assertTrue(r.avanzarRetorno(LocalDateTime.of(2026, 10, 15, 9, 30), almacenes));
        assertEquals(new Ubicacion(27, 14), ta01.getUbicacionActual());
        assertEquals(EstadoOperativo.DISPONIBLE, ta01.getEstadoOperativo());
        assertEquals(EstadoRuta.COMPLETADA, r.getEstado());
    }

    @Test
    void vuelveAlIntermedioMasCercanoSiTieneStockYAlCentralSiNo() {
        Ruta r = rutaEntregada(new Ubicacion(55, 25));   // 4 km del Este, 39 km del central
        assertSame(este, Ruta.almacenDeRetorno(r.puntoFinalEntregas(), almacenes));

        este.setStockActual(0);   // un almacén sin stock no es destino válido (FAQ 10)
        assertSame(central, Ruta.almacenDeRetorno(r.puntoFinalEntregas(), almacenes));
    }

    @Test
    void unVehiculoAveriadoNoQuedaDisponibleAlCompletarSuRuta() {
        Ruta r = rutaEntregada(new Ubicacion(37, 14));
        r.getUnidadTransporte().setEstadoOperativo(EstadoOperativo.AVERIADA);

        r.avanzarRetorno(LocalDateTime.of(2026, 10, 15, 10, 0), almacenes);
        assertEquals(EstadoOperativo.AVERIADA, r.getUnidadTransporte().getEstadoOperativo());
    }
}
