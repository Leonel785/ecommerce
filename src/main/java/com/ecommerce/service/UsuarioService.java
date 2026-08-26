package com.ecommerce.service;

import com.ecommerce.dto.RegistroRequest;
import com.ecommerce.entity.Carrito;
import com.ecommerce.entity.Cliente;
import com.ecommerce.entity.Rol;
import com.ecommerce.entity.Usuario;
import com.ecommerce.exception.ApiException;
import com.ecommerce.repository.CarritoRepository;
import com.ecommerce.repository.ClienteRepository;
import com.ecommerce.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de negocio para la gestión de usuarios y registro de clientes.
 * <p>
 * Centraliza la autenticación mediante verificación de credenciales
 * y el proceso de registro de nuevos clientes con la creación automática
 * de su usuario asociado y carrito de compras inicial.
 * </p>
 */
@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final CarritoRepository carritoRepository;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          ClienteRepository clienteRepository,
                          CarritoRepository carritoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.carritoRepository = carritoRepository;
    }

    /**
     * Autentica un usuario verificando que el nombre de usuario exista
     * y que la contraseña coincida.
     *
     * @param username nombre de usuario.
     * @param password contraseña ingresada.
     * @return usuario autenticado.
     * @throws ApiException si el usuario no existe o la contraseña es incorrecta.
     */
    public Usuario authenticate(String username, String password) {
        Usuario user = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos"));
        if (!user.getPassword().equals(password)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos");
        }
        return user;
    }

    /**
     * Registra un nuevo cliente en el sistema.
     * <p>Pasos ejecutados:</p>
     * <ol>
     *   <li>Verifica que el nombre de usuario no esté en uso.</li>
     *   <li>Verifica que el correo electrónico no esté registrado.</li>
     *   <li>Crea el registro de {@link Usuario} con rol {@link Rol#CLIENTE}.</li>
     *   <li>Crea el registro de {@link Cliente} vinculado al usuario.</li>
     *   <li>Inicializa y vincula un {@link Carrito} de compras vacío.</li>
     * </ol>
     *
     * @param request datos del registro del cliente.
     * @return entidad cliente creada y persistida.
     * @throws ApiException si el nombre de usuario o correo electrónico ya existen.
     */
    @Transactional
    public Cliente register(RegistroRequest request) {
        if (usuarioRepository.findByUsername(request.username().trim()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "El nombre de usuario ya está en uso");
        }
        if (clienteRepository.findByCorreo(request.correo().trim()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "El correo electrónico ya está registrado");
        }

        Usuario usuario = new Usuario(
                request.username().trim(),
                request.password(),
                Rol.CLIENTE
        );
        usuario = usuarioRepository.save(usuario);

        Cliente cliente = new Cliente(
                request.nombres().trim(),
                request.correo().trim(),
                request.direccion() != null ? request.direccion().trim() : "",
                request.telefono() != null ? request.telefono().trim() : "",
                usuario
        );
        cliente = clienteRepository.save(cliente);

        Carrito carrito = new Carrito(cliente);
        carritoRepository.save(carrito);
        cliente.setCarrito(carrito);

        return cliente;
    }
}