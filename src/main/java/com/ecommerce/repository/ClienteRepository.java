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
     * Busca un cliente por su correo electrónico.
     *
     * @param correo correo electrónico a consultar.
     * @return {@link Optional} con el cliente encontrado o vacío.
     */
    Optional<Cliente> findByCorreo(String correo);
}