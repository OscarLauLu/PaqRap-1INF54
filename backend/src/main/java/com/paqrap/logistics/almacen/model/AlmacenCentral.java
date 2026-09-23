package com.paqrap.logistics.almacen.model;

import com.paqrap.logistics.redvial.model.Ubicacion;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Almacén central de abastecimiento permanente e inventario infinito (RF-17).
 * Priorizado como primera salida de cada unidad de transporte (RF-02).
 */
@Entity
@DiscriminatorValue("CENTRAL")
@NoArgsConstructor
public class AlmacenCentral extends Almacen {

    public AlmacenCentral(String codigo, String nombre, Ubicacion ubicacion) {
        // stock infinito: stock_actual queda NULL, exigido por el CHECK de la tabla "almacen" (documento 62)
        super(codigo, nombre, ubicacion, null);
    }

    @Override
    public boolean tieneStockSuficiente(int cantidad) {
        // Almacén central cuenta con abastecimiento permanente/inventario infinito
        return true;
    }

    @Override
    public MovimientoInventario descontarStock(int cantidad, String codigoRuta) {
        MovimientoInventario mov = MovimientoInventario.builder()
                .codigo("DES-" + UUID.randomUUID().toString().substring(0, 8))
                .fechaHora(LocalDateTime.now())
                .tipo(TipoMovimiento.DESCARGA)
                .cantidad(cantidad)
                .almacen(this)
                .codigoRuta(codigoRuta)
                .descripcion("Despacho hacia ruta " + codigoRuta + " desde Almacén Central")
                .build();
        registrarMovimiento(mov);
        return mov;
    }
}
