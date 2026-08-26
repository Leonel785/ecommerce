package com.ecommerce.dto;

import com.ecommerce.entity.DetallePedido;
import java.math.BigDecimal;

/**
 * Objeto de transferencia de datos (DTO) para representar un ítem o detalle dentro de un pedido.
 *
 * @param id             identificador único del detalle de pedido.
 * @param producto       nombre del producto comprado.
 * @param cantidad       cantidad comprada del producto.
 * @param precioUnitario precio al cual se compró cada unidad del producto.
 * @param subtotal       subtotal pagado por esta línea del pedido.
 * @param imagen         nombre de archivo o imagen del producto.
 */
public record DetallePedidoResponse(
        Long id, String producto, Integer cantidad, BigDecimal precioUnitario, BigDecimal subtotal, String imagen
) {
    /**
     * Convierte una entidad {@link DetallePedido} a {@link DetallePedidoResponse}.
     *
     * @param d entidad fuente.
     * @return DTO transformado.
     */
    public static DetallePedidoResponse from(DetallePedido d) {
        return new DetallePedidoResponse(d.getId(), d.getProducto().getNombre(), d.getCantidad(),
                d.getPrecioUnitario(), d.getSubtotal(), d.getProducto().getImagen());
    }
}