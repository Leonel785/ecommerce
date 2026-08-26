package com.ecommerce.controller;

import com.ecommerce.dto.RegistroRequest;
import com.ecommerce.dto.UsuarioResponse;
import com.ecommerce.entity.Cliente;
import com.ecommerce.entity.Rol;
import com.ecommerce.entity.Usuario;
import com.ecommerce.repository.ClienteRepository;
import com.ecommerce.repository.UsuarioRepository;
import com.ecommerce.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST responsable de la autenticación y gestión de sesiones de usuario.
 * <p>
 * Expone los endpoints de login, registro, consulta de la sesión actual y logout.
 * La sesión se mantiene en {@link HttpSession} y se identifica por el atributo
 * {@code usuarioId} junto con el {@code rol} asociado.
 * </p>
 *
 * <p>Endpoints disponibles:</p>
 * <ul>
 *   <li>{@code POST /api/auth/login} — Autentica y crea la sesión.</li>
 *   <li>{@code POST /api/auth/registro} — Registra un nuevo cliente e inicia sesión.</li>
 *   <li>{@code GET  /api/auth/me} — Devuelve los datos del usuario en sesión.</li>
 *   <li>{@code POST /api/auth/logout} — Cierra la sesión actual.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UsuarioService usuarioService;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;

    public AuthController(UsuarioService usuarioService,
                          ClienteRepository clienteRepository,
                          UsuarioRepository usuarioRepository) {
        this.usuarioService = usuarioService;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Autentica al usuario con sus credenciales y crea una nueva sesión HTTP.
     * <p>Si las credenciales son válidas, se almacenan en la sesión el
     * {@code usuarioId}, el {@code username} y el {@code rol} para ser
     * utilizados por los filtros y {@code SessionGuard}.</p>
     *
     * @param request cuerpo con {@code username} y {@code password}.
     * @param session sesión HTTP donde se guardan los atributos de autenticación.
     * @return datos del usuario autenticado (id, username, rol y nombres).
     * @throws com.ecommerce.exception.ApiException si las credenciales son inválidas.
     */
    @PostMapping("/login")
    public UsuarioResponse login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        Usuario user = usuarioService.authenticate(request.username(), request.password());
        session.setAttribute("usuarioId", user.getId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("rol", user.getRol().name());

        String nombres = "Administrador";
        if (user.getRol() == Rol.CLIENTE) {
            nombres = clienteRepository.findByUsuario(user)
                    .map(Cliente::getNombres)
                    .orElse("Cliente");
        }
        return new UsuarioResponse(user.getId(), user.getUsername(), user.getRol().name(), nombres);
    }

    /**
     * Registra un nuevo cliente en el sistema e inicia sesión automáticamente.
     * <p>El nuevo usuario se crea con rol {@link Rol#CLIENTE}, se genera su
     * {@code Cliente} asociado y se inicializa un {@code Carrito} vacío.</p>
     *
     * @param request datos del registro (nombres, correo, username, password, etc.).
     * @param session sesión HTTP donde se persistirán los atributos del usuario.
     * @return datos del usuario recién creado.
     */
    @PostMapping("/registro")
    public UsuarioResponse register(@Valid @RequestBody RegistroRequest request, HttpSession session) {
        Cliente cliente = usuarioService.register(request);
        Usuario user = cliente.getUsuario();
        session.setAttribute("usuarioId", user.getId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("rol", user.getRol().name());
        return new UsuarioResponse(user.getId(), user.getUsername(), user.getRol().name(), cliente.getNombres());
    }

    /**
     * Devuelve la información del usuario actualmente autenticado.
     * <p>Si no existe una sesión activa, retorna un objeto con
     * {@code authenticated: false}. En caso contrario, devuelve los datos
     * básicos (id, username, rol y nombres) del usuario en sesión.</p>
     *
     * @param session sesión HTTP actual.
     * @return respuesta con la información del usuario o estado de no autenticado.
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        if (session.getAttribute("usuarioId") == null) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }
        
        Long userId = (Long) session.getAttribute("usuarioId");
        String username = (String) session.getAttribute("username");
        String rol = (String) session.getAttribute("rol");
        
        String nombres = "Administrador";
        if ("CLIENTE".equals(rol)) {
            nombres = usuarioRepository.findById(userId)
                    .flatMap(clienteRepository::findByUsuario)
                    .map(Cliente::getNombres)
                    .orElse("Cliente");
        }
        
        return ResponseEntity.ok(Map.of(
            "authenticated", true,
            "username", username,
            "rol", rol,
            "nombres", nombres
        ));
    }

    /**
     * Cierra la sesión actual del usuario invalidando todos los atributos
     * almacenados en {@link HttpSession}.
     *
     * @param session sesión HTTP a invalidar.
     * @return mensaje confirmando el cierre de sesión.
     */
    @PostMapping("/logout")
    public Map<String, String> logout(HttpSession session) {
        session.invalidate();
        return Map.of("message", "Sesión cerrada");
    }

    /**
     * Cuerpo de la petición de inicio de sesión.
     *
     * @param username nombre de usuario (no puede estar vacío).
     * @param password contraseña (no puede estar vacía).
     */
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
}