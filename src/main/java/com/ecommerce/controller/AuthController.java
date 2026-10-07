package com.ecommerce.controller;

import com.ecommerce.dto.RegistroRequest;
import com.ecommerce.dto.UsuarioResponse;
import com.ecommerce.entity.Cliente;
import com.ecommerce.entity.Rol;
import com.ecommerce.entity.Usuario;
import com.ecommerce.exception.ApiException;
import com.ecommerce.repository.ClienteRepository;
import com.ecommerce.security.AuthUser;
import com.ecommerce.security.LoginAttemptService;
import com.ecommerce.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST de autenticación (login, registro, usuario actual y logout).
 * <p>
 * Al autenticarse, la identidad ({@link AuthUser} + rol) se guarda en el {@code SecurityContext}
 * de Spring Security, que se persiste en la sesión HTTP. A partir de ahí, la autorización de
 * todas las rutas la resuelve {@code SecurityConfig} y {@code @PreAuthorize}.
 * </p>
 * <p>Medidas de seguridad: límite de intentos de login, rotación del identificador de
 * sesión al autenticarse (anti session-fixation) y registro de eventos de seguridad.</p>
 *
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
    private final LoginAttemptService loginAttempts;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(UsuarioService usuarioService,
                          ClienteRepository clienteRepository,
                          LoginAttemptService loginAttempts,
                          SecurityContextRepository securityContextRepository) {
        this.usuarioService = usuarioService;
        this.clienteRepository = clienteRepository;
        this.loginAttempts = loginAttempts;
        this.securityContextRepository = securityContextRepository;
    }

    /**
     * Autentica al usuario con sus credenciales y deja la identidad en el contexto de seguridad.
     *
     * @param request      cuerpo con {@code username} y {@code password}.
     * @param httpRequest  petición HTTP (IP de origen y rotación de sesión).
     * @param httpResponse respuesta HTTP (necesaria para persistir el contexto en la sesión).
     * @return datos del usuario autenticado (id, username, rol y nombres).
     * @throws ApiException si las credenciales son inválidas o hay demasiados intentos fallidos.
     */
    @PostMapping("/login")
    public UsuarioResponse login(@Valid @RequestBody LoginRequest request,
                                 HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
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
        startSession(httpRequest, httpResponse, user);
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
     * @param request      datos del registro (nombres, correo, username, password, etc.).
     * @param httpRequest  petición HTTP (rotación de sesión).
     * @param httpResponse respuesta HTTP (persistencia del contexto de seguridad).
     * @return datos del usuario recién creado.
     */
    @PostMapping("/registro")
    public UsuarioResponse register(@Valid @RequestBody RegistroRequest request,
                                    HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Cliente cliente = usuarioService.register(request);
        Usuario user = cliente.getUsuario();
        startSession(httpRequest, httpResponse, user);
        return new UsuarioResponse(user.getId(), user.getUsername(), user.getRol().name(), cliente.getNombres());
    }

    /**
     * Devuelve la información del usuario actualmente autenticado (o {@code authenticated:false}).
     *
     * @param user principal de Spring Security; {@code null} si la petición es anónima.
     * @return información del usuario o estado de no autenticado.
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal AuthUser user) {
        if (user == null) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }

        String nombres = "Administrador";
        if (user.rol() == Rol.CLIENTE) {
            nombres = clienteRepository.findByUsuarioId(user.id())
                    .map(Cliente::getNombres)
                    .orElse("Cliente");
        }

        return ResponseEntity.ok(Map.of(
            "authenticated", true,
            "username", user.username(),
            "rol", user.rol().name(),
            "nombres", nombres
        ));
    }

    /**
     * Cierra la sesión: limpia el contexto de seguridad e invalida la sesión HTTP.
     * Es idempotente (no falla si la sesión ya había expirado).
     */
    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request, response, auth);
        return Map.of("message", "Sesión cerrada");
    }

    /**
     * Crea una sesión nueva con identificador rotado (evita session fixation), construye la
     * autenticación con el rol del usuario y la persiste en la sesión.
     */
    private void startSession(HttpServletRequest request, HttpServletResponse response, Usuario user) {
        request.getSession(true);
        request.changeSessionId();

        AuthUser principal = new AuthUser(user.getId(), user.getUsername(), user.getRol());
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRol().name())));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
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
