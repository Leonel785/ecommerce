package com.ecommerce.controller;

import com.ecommerce.dto.RegistroRequest;
import com.ecommerce.dto.UsuarioResponse;
import com.ecommerce.entity.Cliente;
import com.ecommerce.entity.Rol;
import com.ecommerce.entity.Usuario;
import com.ecommerce.exception.ApiException;
import com.ecommerce.repository.ClienteRepository;
import com.ecommerce.repository.UsuarioRepository;
import com.ecommerce.security.LoginAttemptService;
import com.ecommerce.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST responsable de la autenticación y gestión de sesiones de usuario.
 * <p>
 * Expone los endpoints de login, registro, consulta de la sesión actual y logout.
 * La sesión se mantiene en {@link HttpSession} y se identifica por el atributo
 * {@code usuarioId} junto con el {@code rol} asociado.
 * </p>
 * <p>Medidas de seguridad: límite de intentos de login, rotación del identificador de
 * sesión al autenticarse (anti session-fixation) y registro de eventos de seguridad.</p>
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
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UsuarioService usuarioService;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final LoginAttemptService loginAttempts;

    public AuthController(UsuarioService usuarioService,
                          ClienteRepository clienteRepository,
                          UsuarioRepository usuarioRepository,
                          LoginAttemptService loginAttempts) {
        this.usuarioService = usuarioService;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.loginAttempts = loginAttempts;
    }

    /**
     * Autentica al usuario con sus credenciales y crea una nueva sesión HTTP.
     *
     * @param request     cuerpo con {@code username} y {@code password}.
     * @param httpRequest petición HTTP (IP de origen y rotación de sesión).
     * @return datos del usuario autenticado (id, username, rol y nombres).
     * @throws ApiException si las credenciales son inválidas o hay demasiados intentos fallidos.
     */
    @PostMapping("/login")
    public UsuarioResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String key = httpRequest.getRemoteAddr() + "|" + request.username().trim().toLowerCase(Locale.ROOT);
        if (loginAttempts.isBlocked(key)) {
            log.warn("Login bloqueado por demasiados intentos: ip={}", httpRequest.getRemoteAddr());
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos. Inténtalo nuevamente en 15 minutos.");
        }

        Usuario user;
        try {
            user = usuarioService.authenticate(request.username(), request.password());
        } catch (ApiException ex) {
            loginAttempts.recordFailure(key);
            log.warn("Login fallido: ip={}", httpRequest.getRemoteAddr());
            throw ex;
        }
        loginAttempts.reset(key);
        startSession(httpRequest, user);
        log.info("Login correcto: usuarioId={}", user.getId());

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
     *
     * @param request     datos del registro (nombres, correo, username, password, etc.).
     * @param httpRequest petición HTTP (rotación de sesión).
     * @return datos del usuario recién creado.
     */
    @PostMapping("/registro")
    public UsuarioResponse register(@Valid @RequestBody RegistroRequest request, HttpServletRequest httpRequest) {
        Cliente cliente = usuarioService.register(request);
        Usuario user = cliente.getUsuario();
        startSession(httpRequest, user);
        return new UsuarioResponse(user.getId(), user.getUsername(), user.getRol().name(), cliente.getNombres());
    }

    /**
     * Devuelve la información del usuario actualmente autenticado.
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
     * Crea una sesión nueva con identificador rotado (evita session fixation)
     * y guarda en ella la identidad del usuario.
     */
    private void startSession(HttpServletRequest httpRequest, Usuario user) {
        httpRequest.getSession(true);
        httpRequest.changeSessionId();
        HttpSession session = httpRequest.getSession(false);
        session.setAttribute("usuarioId", user.getId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("rol", user.getRol().name());
    }

    /**
     * Cuerpo de la petición de inicio de sesión.
     *
     * @param username nombre de usuario (no puede estar vacío).
     * @param password contraseña (no puede estar vacía).
     */
    public record LoginRequest(@NotBlank @Size(max = 100) String username,
                               @NotBlank @Size(max = 128) String password) {}
}
