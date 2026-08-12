package com.ecommerce.util;

import com.ecommerce.entity.Cliente;
import com.ecommerce.exception.ApiException;
import com.ecommerce.repository.ClienteRepository;
import com.ecommerce.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class SessionGuard {
    private static UsuarioRepository usuarioRepository;
    private static ClienteRepository clienteRepository;

    public SessionGuard(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository) {
        SessionGuard.usuarioRepository = usuarioRepository;
        SessionGuard.clienteRepository = clienteRepository;
    }

    public static void requireAdmin(HttpSession session) {
        requireRole(session, "ADMIN");
    }

    public static Cliente requireClient(HttpSession session) {
        requireRole(session, "CLIENTE");
        Long userId = (Long) session.getAttribute("usuarioId");
        return clienteRepository.findByUsuario(usuarioRepository.findById(userId)
                        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Sesión inválida")))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
    }

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