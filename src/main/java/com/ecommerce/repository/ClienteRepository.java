package com.ecommerce.repository;

import com.ecommerce.entity.Cliente;
import com.ecommerce.entity.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA para realizar operaciones de persistencia sobre la entidad {@link Cliente}.
 */
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    /**
     * Busca un cliente por su cuenta de usuario vinculada.
     *
     * @param usuario cuenta de usuario.
     * @return {@link Optional} con el cliente encontrado o vacío.
     */
    Optional<Cliente> findByUsuario(Usuario usuario);

    /**
     * Busca el cliente asociado al id de un usuario (usado con el usuario autenticado).
     *
     * @param usuarioId id del usuario.
     * @return {@link Optional} con el cliente o vacío.
     */
    Optional<Cliente> findByUsuarioId(Long usuarioId);

    /**
     * Busca un cliente por el índice ciego (HMAC) de su correo. El correo se guarda
     * cifrado, por lo que no se puede consultar directamente por su valor.
     *
     * @param correoHash HMAC-SHA256 del correo normalizado.
     * @return {@link Optional} con el cliente encontrado o vacío.
     */
    Optional<Cliente> findByCorreoHash(String correoHash);
}