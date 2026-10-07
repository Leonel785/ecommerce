package com.ecommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Objeto de transferencia de datos (DTO) para la solicitud de registro de nuevos clientes.
 *
 * @param nombres   nombres completos del cliente (obligatorio).
 * @param correo    correo electrónico de contacto (obligatorio y con formato válido).
 * @param direccion dirección física de envío (opcional).
 * @param telefono  teléfono de contacto (opcional).
 * @param username  nombre de usuario único para inicio de sesión (obligatorio).
 * @param password  contraseña de acceso (obligatoria).
 */
public record RegistroRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 140, message = "El nombre no puede superar 140 caracteres") String nombres,
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo debe ser válido")
        @Size(max = 160, message = "El correo no puede superar 160 caracteres") String correo,
        @Size(max = 220, message = "La dirección no puede superar 220 caracteres") String direccion,
        @Size(max = 40, message = "El teléfono no puede superar 40 caracteres")
        @Pattern(regexp = "^[0-9+()\\-\\s]*$", message = "El teléfono solo admite números, espacios, + ( ) y -") String telefono,
        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 4, max = 50, message = "El usuario debe tener entre 4 y 50 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "El usuario solo admite letras, números, punto, guion y guion bajo") String username,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 10, max = 72, message = "La contraseña debe tener entre 10 y 72 caracteres")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "La contraseña debe combinar letras y números") String password
) {}
