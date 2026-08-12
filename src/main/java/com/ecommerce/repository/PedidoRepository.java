package com.ecommerce.repository;

import com.ecommerce.entity.Pedido;
import com.ecommerce.entity.Cliente;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByClienteOrderByFechaDesc(Cliente cliente);
}