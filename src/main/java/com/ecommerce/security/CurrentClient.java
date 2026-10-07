package com.ecommerce.security;

import com.ecommerce.entity.Cliente;
import com.ecommerce.exception.ApiException;
import com.ecommerce.repository.ClienteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Resuelve el {@link Cliente} asociado al usuario autenticado. Sustituye al antiguo
 * {@code SessionGuard.requireClient}: la comprobación del ROL ya la hacen las reglas de
 * Spring Security; aquí solo se obtiene la entidad de negocio.
 */
@Component
public class CurrentClient {
    private final ClienteRepository clienteRepository;

    public CurrentClient(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    /**
     * @param user usuario autenticado (principal de Spring Security).
     * @return cliente asociado a ese usuario.
     * @throws ApiException 401 si no hay usuario o 404 si el usuario no tiene perfil de cliente.
     */
    public Cliente from(AuthUser user) {
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión");
        }
        return clienteRepository.findByUsuarioId(user.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
    }
}
