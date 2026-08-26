package com.ecommerce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA que representa el carrito de compras de un cliente.
 * <p>
 * Mantiene la relación uno a uno con {@link Cliente} y contiene una colección
 * de {@link DetalleCarrito} correspondiente a los productos seleccionados.
 * </p>
 */
@Entity
@Table(name = "carrito")
public class Carrito {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @OneToOne
    @JoinColumn(name = "cliente_id", nullable = false, unique = true)
    private Cliente cliente;

    @OneToMany(mappedBy = "carrito", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleCarrito> detalles = new ArrayList<>();

    public Carrito() {}

    /**
     * Crea un nuevo carrito de compras para el cliente especificado.
     * Inicializa la fecha actual y establece el total inicial en cero.
     *
     * @param cliente cliente propietario del carrito.
     */
    public Carrito(Cliente cliente) {
        this.cliente = cliente;
        this.fecha = LocalDateTime.now();
        this.total = BigDecimal.ZERO;
    }

    public Long getId() { return id; }
    public LocalDateTime getFecha() { return fecha; }
    public BigDecimal getTotal() { return total; }
    public Cliente getCliente() { return cliente; }
    public List<DetalleCarrito> getDetalles() { return detalles; }
    public void setId(Long id) { this.id = id; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
}