package com.paqrap.logistics.pedidos.dto;

import com.paqrap.logistics.pedidos.model.TipoEntrega;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la modificación de un pedido existente en estado REGISTRADO (RF-35).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarPedidoDTO {

    @Positive(message = "La cantidad debe ser mayor a cero")
    private Integer cantidadUnidades;

    @jakarta.validation.constraints.Min(value = 0, message = "La coordenada X debe ser mayor o igual a 0")
    @jakarta.validation.constraints.Max(value = 70, message = "La coordenada X debe estar dentro de la retícula (0..70)")
    private Integer destinoX;

    @jakarta.validation.constraints.Min(value = 0, message = "La coordenada Y debe ser mayor o igual a 0")
    @jakarta.validation.constraints.Max(value = 50, message = "La coordenada Y debe estar dentro de la retícula (0..50)")
    private Integer destinoY;

    private TipoEntrega tipoEntrega;
}
