package com.ecommerce.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Objeto de transferencia de datos (DTO) para solicitudes de adición o
 * actualización de ítems dentro del carrito de compras.
 *
 * @param productoId identificador único del producto a agregar o modificar.
 * @param cantidad   cantidad del producto deseada (debe ser al menos 1).
 */
public record CarritoRequest(
        @NotNull(message = "El producto es obligatorio") Long productoId,
        @NotNull @Min(value = 1, message = "La cantidad debe ser mayor a 0") Integer cantidad
) {}