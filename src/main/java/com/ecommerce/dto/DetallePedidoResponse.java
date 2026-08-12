package com.ecommerce.dto;

import com.ecommerce.entity.DetallePedido;
import java.math.BigDecimal;

public record DetallePedidoResponse(
        Long id, String producto, Integer cantidad, BigDecimal precioUnitario, BigDecimal subtotal, String imagen
) {
    public static DetallePedidoResponse from(DetallePedido d) {
        return new DetallePedidoResponse(d.getId(), d.getProducto().getNombre(), d.getCantidad(),
                d.getPrecioUnitario(), d.getSubtotal(), d.getProducto().getImagen());
    }
}