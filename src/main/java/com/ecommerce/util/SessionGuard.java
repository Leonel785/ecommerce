package com.ecommerce.util;

import com.ecommerce.entity.Cliente;
import com.ecommerce.exception.ApiException;
import com.ecommerce.repository.ClienteRepository;
import com.ecommerce.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Componente utilitario de seguridad para verificar la sesión HTTP activa
 * y autorizar operaciones según el rol del usuario.
 */
@Component
public class SessionGuard {
    private static UsuarioRepository usuarioRepository;
    private static ClienteRepository clienteRepository;

    public SessionGuard(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository) {
        SessionGuard.usuarioRepository = usuarioRepository;
        SessionGuard.clienteRepository = clienteRepository;
    }

    /**
     * Valida que la sesión actual pertenezca a un usuario con rol {@code ADMIN}.
     *
     * @param session sesión HTTP actual.
     * @throws ApiException si el usuario no ha iniciado sesión o no es administrador.
     */
    public static void requireAdmin(HttpSession session) {
        requireRole(session, "ADMIN");
    }

    /**
     * Valida que la sesión actual pertenezca a un usuario con rol {@code CLIENTE}
     * y retorna la entidad {@link Cliente} asociada a dicha sesión.
     *
     * @param session sesión HTTP actual.
     * @return entidad cliente asociada.
     * @throws ApiException si la sesión no es válida, el usuario no es cliente o no se encuentra la entidad.
     */
    public static Cliente requireClient(HttpSession session) {
        requireRole(session, "CLIENTE");
        Long userId = (Long) session.getAttribute("usuarioId");
        return clienteRepository.findByUsuario(usuarioRepository.findById(userId)
                        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Sesión inválida")))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
    }

    /**
     * Comprueba internamente los atributos de la sesión contra el rol requerido.
     *
     * @param session sesión HTTP actual.
     * @param role    nombre del rol requerido.
     * @throws ApiException si no existe atributo {@code usuarioId} o si el rol no coincide.
     */
    private static void requireRole(HttpSession session, String role) {
        Object currentRole = session.getAttribute("rol");
        if (session.getAttribute("usuarioId") == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión");
        }
        if (!role.equals(currentRole)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No tienes permisos para esta operación");
        }
    }
}