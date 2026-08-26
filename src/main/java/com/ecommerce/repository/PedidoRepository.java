package com.ecommerce.repository;

import com.ecommerce.entity.Pedido;
import com.ecommerce.entity.Cliente;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA para realizar operaciones de persistencia sobre la entidad {@link Pedido}.
 */
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    /**
     * Devuelve los pedidos pertenecientes a un cliente especifico, ordenados por fecha descendente.
     *
     * @param cliente cliente propietario de los pedidos.
     * @return lista de pedidos ordenados desde el más reciente.
     */
    List<Pedido> findByClienteOrderByFechaDesc(Cliente cliente);
}