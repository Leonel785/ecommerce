package com.ecommerce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA que representa un pedido de compra realizado por un {@link Cliente}.
 * <p>
 * Contiene la fecha de confirmación, el monto total pagado, el {@link EstadoPedido}
 * actual y la lista de {@link DetallePedido}s asociados.
 * </p>
 */
@Entity
@Table(name = "pedido")
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPedido estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedido> detalles = new ArrayList<>();

    public Pedido() {}

    public Long getId() { return id; }
    public LocalDateTime getFecha() { return fecha; }
    public BigDecimal getTotal() { return total; }
    public EstadoPedido getEstado() { return estado; }
    public Cliente getCliente() { return cliente; }
    public List<DetallePedido> getDetalles() { return detalles; }
    public void setId(Long id) { this.id = id; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public void setEstado(EstadoPedido estado) { this.estado = estado; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
}