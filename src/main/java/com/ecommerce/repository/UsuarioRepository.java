package com.ecommerce.repository;

import com.ecommerce.entity.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA para realizar operaciones de persistencia sobre la entidad {@link Usuario}.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    /**
     * Busca una cuenta de usuario por su nombre de usuario (username).
     *
     * @param username nombre de usuario a consultar.
     * @return {@link Optional} con el usuario encontrado o vacío si no existe.
     */
    Optional<Usuario> findByUsername(String username);
}