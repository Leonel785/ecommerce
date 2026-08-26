package com.ecommerce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Entidad JPA que representa la línea o ítem de un {@link Pedido} confirmado.
 * <p>
 * Congela la cantidad comprada, el precio unitario y el subtotal al momento en que
 * se generó la compra, garantizando el histórico de precios sin depender de modificaciones posteriores al catálogo.
 * </p>
 */
@Entity
@Table(name = "detalle_pedido")
public class DetallePedido {
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
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    public DetallePedido() {}

    /**
     * Construye un detalle de pedido congelando los datos del producto e importe.
     *
     * @param pedido         pedido al cual pertenece este detalle.
     * @param producto       producto comprado.
     * @param cantidad       cantidad comprada.
     * @param precioUnitario precio unitario fijado en la compra.
     */
    public DetallePedido(Pedido pedido, Producto producto, Integer cantidad, BigDecimal precioUnitario) {
        this.pedido = pedido;
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.subtotal = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    public Long getId() { return id; }
    public Integer getCantidad() { return cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public BigDecimal getSubtotal() { return subtotal; }
    public Pedido getPedido() { return pedido; }
    public Producto getProducto() { return producto; }
    public void setId(Long id) { this.id = id; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }
    public void setProducto(Producto producto) { this.producto = producto; }
}