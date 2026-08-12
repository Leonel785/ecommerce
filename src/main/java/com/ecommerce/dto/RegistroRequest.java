package com.ecommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegistroRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombres,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo debe ser válido") String correo,
        String direccion,
        String telefono,
        @NotBlank(message = "El nombre de usuario es obligatorio") String username,
        @NotBlank(message = "La contraseña es obligatoria") String password
) {}
