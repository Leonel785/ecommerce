package com.ecommerce.dto;

import com.ecommerce.entity.Producto;
import java.math.BigDecimal;

public record ProductoResponse(Long id, String nombre, String categoria, BigDecimal precio, Integer stock, String imagen) {
    public static ProductoResponse from(Producto p) {
        return new ProductoResponse(p.getId(), p.getNombre(), p.getCategoria(), p.getPrecio(), p.getStock(), p.getImagen());
    }
}