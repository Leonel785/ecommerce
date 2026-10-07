package com.ecommerce.entity;

import com.ecommerce.security.EncryptedStringConverter;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA que representa a un cliente registrado en la tienda.
 * <p>
 * Mantiene la información personal (nombres, correo, dirección, teléfono) y sus
 * relaciones con la cuenta de {@link Usuario}, sus {@link Pedido}s y su {@link Carrito}.
 * </p>
 */
@Entity
@Table(name = "cliente")
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Datos personales (PII) cifrados en reposo con AES-256-GCM (ver CryptoService).
    // Las longitudes son mayores porque el texto cifrado en Base64 ocupa más que el original.
    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 768)
    private String nombres;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 768)
    private String correo;

    /** Índice ciego (HMAC-SHA256) del correo: permite verificar unicidad sin guardar el correo en claro. */
    @Column(name = "correo_hash", unique = true, length = 64)
    private String correoHash;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "direccion", length = 1024)
    private String direccion;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 256)
    private String telefono;

    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @OneToMany(mappedBy = "cliente")
    private List<Pedido> pedidos = new ArrayList<>();

    @OneToOne(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private Carrito carrito;

    public Cliente() {}

    /**
     * Construye un cliente con los datos obligatorios y la cuenta de usuario asociada.
     *
     * @param nombres   nombres completos del cliente.
     * @param correo    correo electrónico.
     * @param direccion dirección física.
     * @param telefono  teléfono de contacto.
     * @param usuario   cuenta de usuario asignada.
     */
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
    public String getCorreoHash() { return correoHash; }
    public String getDireccion() { return direccion; }
    public String getTelefono() { return telefono; }
    public Usuario getUsuario() { return usuario; }
    public List<Pedido> getPedidos() { return pedidos; }
    public Carrito getCarrito() { return carrito; }
    public void setId(Long id) { this.id = id; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public void setCorreo(String correo) { this.correo = correo; }
    public void setCorreoHash(String correoHash) { this.correoHash = correoHash; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public void setCarrito(Carrito carrito) { this.carrito = carrito; }
}