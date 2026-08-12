package com.ecommerce.dto;

import com.ecommerce.entity.DetalleCarrito;
import java.math.BigDecimal;

public record DetalleCarritoResponse(
        Long id, Long productoId, String producto, Integer cantidad,
        BigDecimal precioUnitario, BigDecimal subtotal, Integer stockDisponible, String imagen
) {
    public static DetalleCarritoResponse from(DetalleCarrito d) {
        return new DetalleCarritoResponse(
                d.getId(), d.getProducto().getId(), d.getProducto().getNombre(),
                d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal(), d.getProducto().getStock(),
                d.getProducto().getImagen()
        );
    }
}