package com.ecommerce.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Objeto de transferencia de datos (DTO) para solicitudes de creación y edición de productos.
 *
 * @param nombre    nombre del producto (obligatorio).
 * @param categoria categoría del producto (obligatoria).
 * @param precio    precio unitario del producto (obligatorio, mayor a 0).
 * @param stock     cantidad disponible en stock (obligatorio, no negativo).
 * @param imagen    nombre de archivo de la imagen asignada (opcional).
 */
public record ProductoRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotBlank(message = "La categoría es obligatoria") String categoria,
        @NotNull @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0") BigDecimal precio,
        @NotNull @Min(value = 0, message = "El stock no puede ser negativo") Integer stock,
        String imagen
) {}