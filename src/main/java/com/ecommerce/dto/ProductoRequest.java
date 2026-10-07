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
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 160, message = "El nombre no puede superar 160 caracteres") String nombre,
        @NotBlank(message = "La categoría es obligatoria") @Size(max = 100, message = "La categoría no puede superar 100 caracteres") String categoria,
        @NotNull @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
        @Digits(integer = 10, fraction = 2, message = "Precio fuera de rango") BigDecimal precio,
        @NotNull @Min(value = 0, message = "El stock no puede ser negativo")
        @Max(value = 1_000_000, message = "Stock fuera de rango") Integer stock,
        @Pattern(regexp = "^$|^[A-Za-z0-9-]{1,64}\\.(jpg|jpeg|png|webp)$", message = "Nombre de imagen inválido") String imagen
) {}