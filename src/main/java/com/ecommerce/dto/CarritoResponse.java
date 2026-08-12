package com.ecommerce.dto;

import com.ecommerce.entity.Carrito;
import java.math.BigDecimal;
import java.util.List;

public record CarritoResponse(Long id, BigDecimal total, List<DetalleCarritoResponse> detalles) {
    public static CarritoResponse from(Carrito carrito) {
        return new CarritoResponse(carrito.getId(), carrito.getTotal(),
                carrito.getDetalles().stream().map(DetalleCarritoResponse::from).toList());
    }
}