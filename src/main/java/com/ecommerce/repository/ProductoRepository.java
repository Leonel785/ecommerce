package com.ecommerce.repository;

import com.ecommerce.entity.Producto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByNombreContainingIgnoreCase(String nombre);
    List<Producto> findByCategoriaIgnoreCase(String categoria);
    List<Producto> findByNombreContainingIgnoreCaseAndCategoriaIgnoreCase(String nombre, String categoria);
}