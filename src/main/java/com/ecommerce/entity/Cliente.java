package com.ecommerce.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cliente")
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 140)
    private String nombres;

    @Column(nullable = false, unique = true, length = 160)
    private String correo;

    @Column(name = "direccion", length = 220)
    private String direccion;

    @Column(length = 40)
    private String telefono;

    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @OneToMany(mappedBy = "cliente")
    private List<Pedido> pedidos = new ArrayList<>();

    @OneToOne(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private Carrito carrito;

    public Cliente() {}

    public Cliente(String nombres, String correo, String direccion, String telefono, Usuario usuario) {
        this.nombres = nombres;
        this.correo = correo;
        this.direccion = direccion;
        this.telefono = telefono;
        this.usuario = usuario;
    }

    public Long getId() { return id; }
    public String getNombres() { return nombres; }
    public String getCorreo() { return correo; }
    public String getDireccion() { return direccion; }
    public String getTelefono() { return telefono; }
    public Usuario getUsuario() { return usuario; }
    public List<Pedido> getPedidos() { return pedidos; }
    public Carrito getCarrito() { return carrito; }
    public void setId(Long id) { this.id = id; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public void setCorreo(String correo) { this.correo = correo; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public void setCarrito(Carrito carrito) { this.carrito = carrito; }
}