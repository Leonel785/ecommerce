package com.ecommerce.repository;

import com.ecommerce.entity.Carrito;
import com.ecommerce.entity.Cliente;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA para realizar operaciones de persistencia sobre la entidad {@link Carrito}.
 */
public interface CarritoRepository extends JpaRepository<Carrito, Long> {
    /**
     * Busca el carrito asignado a un cliente.
     *
     * @param cliente entidad cliente buscada.
     * @return {@link Optional} con el carrito encontrado o vacío si no posee uno.
     */
    Optional<Carrito> findByCliente(Cliente cliente);
}