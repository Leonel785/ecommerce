package com.ecommerce.dto;

import com.ecommerce.entity.Producto;
import java.math.BigDecimal;

/**
 * Objeto de transferencia de datos (DTO) para enviar información de un producto al cliente.
 *
 * @param id        identificador único del producto.
 * @param nombre    nombre comercial del producto.
 * @param categoria categoría del producto.
 * @param precio    precio unitario.
 * @param stock     cantidad disponible en inventario.
 * @param imagen    nombre de archivo de la imagen asignada.
 */
public record ProductoResponse(Long id, String nombre, String categoria, BigDecimal precio, Integer stock, String imagen) {
    /**
     * Convierte una entidad {@link Producto} a {@link ProductoResponse}.
     *
     * @param p entidad fuente.
     * @return DTO transformado.
     */
    public static ProductoResponse from(Producto p) {
        return new ProductoResponse(p.getId(), p.getNombre(), p.getCategoria(), p.getPrecio(), p.getStock(), p.getImagen());
    }
}