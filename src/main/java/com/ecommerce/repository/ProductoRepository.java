package com.ecommerce.repository;

import com.ecommerce.entity.Producto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA para realizar operaciones de persistencia y consultas personalizadas sobre la entidad {@link Producto}.
 */
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    /**
     * Busca productos cuyo nombre contenga el texto indicado (sin distinguir mayúsculas/minúsculas).
     *
     * @param nombre término de búsqueda en el nombre del producto.
     * @return lista de productos coincidentes.
     */
    List<Producto> findByNombreContainingIgnoreCase(String nombre);

    /**
     * Busca productos pertenecientes a una categoría específica (sin distinguir mayúsculas/minúsculas).
     *
     * @param categoria nombre de la categoría a filtrar.
     * @return lista de productos en la categoría.
     */
    List<Producto> findByCategoriaIgnoreCase(String categoria);

    /**
     * Busca productos combinando filtro por coincidencia parcial de nombre y categoría exacta.
     *
     * @param nombre    término de búsqueda en el nombre.
     * @param categoria nombre de la categoría.
     * @return lista de productos que cumplen ambos criterios.
     */
    List<Producto> findByNombreContainingIgnoreCaseAndCategoriaIgnoreCase(String nombre, String categoria);
}