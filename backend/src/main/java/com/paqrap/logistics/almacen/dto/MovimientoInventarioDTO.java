package com.paqrap.logistics.almacen.dto;

import com.paqrap.logistics.almacen.model.TipoMovimiento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoInventarioDTO {
    private String codigo;
    private String almacenCodigo;
    private String almacenNombre;
    private LocalDateTime fechaHora;
    private TipoMovimiento tipo;
    private int cantidad;
    private String codigoRuta;
    private String descripcion;
}
