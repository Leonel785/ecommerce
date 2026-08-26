package com.ecommerce.dto;

/**
 * Objeto de transferencia de datos (DTO) para enviar los datos de autenticación del usuario.
 *
 * @param id       identificador único del usuario.
 * @param username nombre de usuario.
 * @param rol      rol asignado al usuario (ej. CLIENTE o ADMIN).
 * @param nombres  nombre completo del cliente o identificador del administrador.
 */
public record UsuarioResponse(Long id, String username, String rol, String nombres) {}