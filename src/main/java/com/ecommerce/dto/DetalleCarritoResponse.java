package com.ecommerce.dto;

import com.ecommerce.entity.DetalleCarrito;
import java.math.BigDecimal;

/**
 * Objeto de transferencia de datos (DTO) para representar la respuesta de un detalle dentro del carrito.
 *
 * @param id               identificador único del detalle.
 * @param productoId       identificador del producto asociado.
 * @param producto         nombre del producto.
 * @param cantidad         cantidad seleccionada por el cliente.
 * @param precioUnitario   precio unitario del producto al momento de estar en el carrito.
 * @param subtotal         subtotal calculado para esta línea (precioUnitario * cantidad).
 * @param stockDisponible stock actualmente disponible del producto en inventario.
 * @param imagen           nombre de archivo o URL de la imagen del producto.
 */
public record DetalleCarritoResponse(
        Long id, Long productoId, String producto, Integer cantidad,
        BigDecimal precioUnitario, BigDecimal subtotal, Integer stockDisponible, String imagen
) {
    /**
     * Convierte una entidad {@link DetalleCarrito} a {@link DetalleCarritoResponse}.
     *
     * @param d entidad fuente.
     * @return DTO transformado.
     */
    public static DetalleCarritoResponse from(DetalleCarrito d) {
        return new DetalleCarritoResponse(
                d.getId(), d.getProducto().getId(), d.getProducto().getNombre(),
                d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal(), d.getProducto().getStock(),
                d.getProducto().getImagen()
        );
    }
}