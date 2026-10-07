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
import com.ecommerce.security.CryptoService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;
    private final CryptoService cryptoService;
    /** BCrypt solo procesa los primeros 72 bytes de la contraseña. */
    private static final int MAX_PASSWORD_BYTES = 72;
    /** Hash señuelo para igualar el tiempo de respuesta cuando el usuario no existe. */
    private final String dummyHash;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          ClienteRepository clienteRepository,
                          CarritoRepository carritoRepository,
                          PasswordEncoder passwordEncoder,
                          CryptoService cryptoService) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.carritoRepository = carritoRepository;
        this.passwordEncoder = passwordEncoder;
        this.cryptoService = cryptoService;
        this.dummyHash = passwordEncoder.encode("contraseña-señuelo-no-valida");
    }

    private static boolean isBcryptHash(String value) {
        return value != null && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
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
        Optional<Usuario> found = usuarioRepository.findByUsername(username == null ? "" : username.trim());
        boolean validLength = password != null
                && password.getBytes(StandardCharsets.UTF_8).length <= MAX_PASSWORD_BYTES;

        if (found.isEmpty() || !validLength) {
            // Se ejecuta igualmente un BCrypt para que el tiempo de respuesta no revele si el usuario existe.
            passwordEncoder.matches("señuelo", dummyHash);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos");
        }

        Usuario user = found.get();
        String stored = user.getPassword();
        boolean valid;
        if (isBcryptHash(stored)) {
            valid = passwordEncoder.matches(password, stored);
        } else {
            // Contraseña heredada en texto plano: comparación en tiempo constante y migración a hash.
            valid = MessageDigest.isEqual(
                    stored.getBytes(StandardCharsets.UTF_8), password.getBytes(StandardCharsets.UTF_8));
            if (valid) {
                user.setPassword(passwordEncoder.encode(password));
                usuarioRepository.save(user);
            }
        }
        if (!valid) {
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
        if (request.password().getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La contraseña es demasiado larga (máximo 72 bytes)");
        }
        if (usuarioRepository.findByUsername(request.username().trim()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "El nombre de usuario ya está en uso");
        }
        String correoHash = cryptoService.blindIndex(request.correo());
        if (clienteRepository.findByCorreoHash(correoHash).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "El correo electrónico ya está registrado");
        }

        Usuario usuario = new Usuario(
                request.username().trim(),
                passwordEncoder.encode(request.password()),
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
        cliente.setCorreoHash(correoHash);
        cliente = clienteRepository.save(cliente);

        Carrito carrito = new Carrito(cliente);
        carritoRepository.save(carrito);
        cliente.setCarrito(carrito);

        return cliente;
    }
}