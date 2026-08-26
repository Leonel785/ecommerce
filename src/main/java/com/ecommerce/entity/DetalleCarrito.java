package com.ecommerce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Entidad JPA que representa un ítem o producto agregado dentro de un {@link Carrito}.
 * <p>
 * Contiene la cantidad seleccionada, el precio unitario del producto y calcula
 * automáticamente el subtotal correspondiente.
 * </p>
 */
@Entity
@Table(name = "detalle_carrito")
public class DetalleCarrito {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrito_id", nullable = false)
    private Carrito carrito;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    public DetalleCarrito() {}

    /**
     * Construye un detalle de carrito asociándolo al carrito correspondiente, al producto y a la cantidad solicitada.
     * Ejecuta automáticamente el método {@link #recalculate()} para establecer el precio unitario e importe subtotal.
     *
     * @param carrito  carrito de compras asociador.
     * @param producto producto seleccionado.
     * @param cantidad cantidad de unidades.
     */
    public DetalleCarrito(Carrito carrito, Producto producto, Integer cantidad) {
        this.carrito = carrito;
        this.producto = producto;
        this.cantidad = cantidad;
        recalculate();
    }

    /**
     * Recalcula el precio unitario actual del producto y actualiza el subtotal (precioUnitario * cantidad).
     */
    public void recalculate() {
        this.precioUnitario = producto.getPrecio();
        this.subtotal = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    public Long getId() { return id; }
    public Integer getCantidad() { return cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public BigDecimal getSubtotal() { return subtotal; }
    public Carrito getCarrito() { return carrito; }
    public Producto getProducto() { return producto; }
    public void setId(Long id) { this.id = id; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public void setCarrito(Carrito carrito) { this.carrito = carrito; }
    public void setProducto(Producto producto) { this.producto = producto; }
}