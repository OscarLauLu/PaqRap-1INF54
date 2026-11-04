package com.paqrap.logistics.pedidos.dto;

import com.paqrap.logistics.pedidos.model.TipoEntrega;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para el registro de un nuevo pedido (RF-26, RF-27, RF-28, RF-32).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearPedidoDTO {

    @NotBlank(message = "El identificador o nombre del cliente es obligatorio")
    private String idCliente;

    @NotBlank(message = "El nombre del cliente es obligatorio")
    private String nombreCliente;

    @Min(value = 0, message = "La coordenada X debe ser mayor o igual a 0")
    @jakarta.validation.constraints.Max(value = 70, message = "La coordenada X debe estar dentro de la retícula (0..70)")
    private int destinoX;

    @Min(value = 0, message = "La coordenada Y debe ser mayor o igual a 0")
    @jakarta.validation.constraints.Max(value = 50, message = "La coordenada Y debe estar dentro de la retícula (0..50)")
    private int destinoY;

    @Positive(message = "La cantidad de unidades debe ser un entero positivo mayor a cero")
    private int cantidadUnidades;

    @NotNull(message = "El tipo de entrega es obligatorio (REGULAR_36H, PRIORIZADA_4H, 8H, 12H, 18H)")
    private TipoEntrega tipoEntrega;
}
