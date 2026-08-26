package com.ecommerce.dto;

import com.ecommerce.entity.Carrito;
import java.math.BigDecimal;
import java.util.List;

/**
 * Objeto de transferencia de datos (DTO) para la respuesta con la información completa del carrito.
 *
 * @param id       identificador único del carrito.
 * @param total    monto total acumulado del carrito.
 * @param detalles lista de detalles o productos incluidos en el carrito.
 */
public record CarritoResponse(Long id, BigDecimal total, List<DetalleCarritoResponse> detalles) {
    /**
     * Construye un {@link CarritoResponse} a partir de la entidad {@link Carrito}.
     *
     * @param carrito entidad fuente.
     * @return DTO transformado.
     */
    public static CarritoResponse from(Carrito carrito) {
        return new CarritoResponse(carrito.getId(), carrito.getTotal(),
                carrito.getDetalles().stream().map(DetalleCarritoResponse::from).toList());
    }
}