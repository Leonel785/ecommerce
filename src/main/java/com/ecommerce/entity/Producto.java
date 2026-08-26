package com.ecommerce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Entidad JPA que representa un producto dentro del catálogo del e-commerce.
 * <p>
 * Contiene el nombre, categoría, precio, unidades en stock y la referencia de imagen del producto.
 * </p>
 */
@Entity
@Table(name = "producto")
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String categoria;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private Integer stock;

    @Column(name = "imagen", length = 255)
    private String imagen;

    public Producto() {}

    /**
     * Construye una instancia de producto con los atributos especificados.
     *
     * @param nombre    nombre del producto.
     * @param categoria categoría del producto.
     * @param precio    precio unitario.
     * @param stock     cantidad disponible en inventario.
     * @param imagen    nombre del archivo de imagen asignado (opcional).
     */
    public Producto(String nombre, String categoria, BigDecimal precio, Integer stock, String imagen) {
        this.nombre = nombre;
        this.categoria = categoria;
        this.precio = precio;
        this.stock = stock;
        this.imagen = imagen;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCategoria() { return categoria; }
    public BigDecimal getPrecio() { return precio; }
    public Integer getStock() { return stock; }
    public String getImagen() { return imagen; }
    
    public void setId(Long id) { this.id = id; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public void setStock(Integer stock) { this.stock = stock; }
    public void setImagen(String imagen) { this.imagen = imagen; }
}