package com.ecommerce.entity;

import jakarta.persistence.*;

/**
 * Entidad JPA que representa una cuenta de usuario para autenticación en el sistema.
 * <p>
 * Contiene las credenciales de acceso (username y password) y el {@link Rol} asignado.
 * </p>
 */
@Entity
@Table(name = "usuario")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(nullable = false, length = 120)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    public Usuario() {}

    /**
     * Construye una instancia de Usuario con sus credenciales y rol.
     *
     * @param username nombre de usuario único.
     * @param password contraseña de acceso.
     * @param rol      rol de permisos asignado.
     */
    public Usuario(String username, String password, Rol rol) {
        this.username = username;
        this.password = password;
        this.rol = rol;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public Rol getRol() { return rol; }
    public void setId(Long id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setRol(Rol rol) { this.rol = rol; }
}