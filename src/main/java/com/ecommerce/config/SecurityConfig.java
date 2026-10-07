package com.ecommerce.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Configuración central de Spring Security.
 * <ul>
 *   <li><b>Autorización por ruta y rol</b> (ADMIN / CLIENTE) con política "denegar por defecto":
 *       toda ruta no listada explícitamente responde 403.</li>
 *   <li><b>Autorización por método</b> con {@code @PreAuthorize} en los controladores
 *       (segunda barrera, defensa en profundidad).</li>
 *   <li>Identidad en el {@code SecurityContext}, persistida en la sesión HTTP.</li>
 *   <li>Hash de contraseñas BCrypt (factor 12).</li>
 *   <li>CSRF con token en cookie y cabeceras de seguridad (CSP, HSTS, X-Frame-Options...).</li>
 *   <li>Respuestas 401/403 en JSON para {@code /api/**} y redirecciones para las páginas.</li>
 * </ul>
 * El inicio de sesión lo realiza {@code AuthController} (con límite de intentos) y deja el
 * usuario autenticado en el contexto; por eso se desactivan el formulario de login y HTTP Basic.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String CSP = String.join("; ",
            "default-src 'self'",
            "script-src 'self' 'unsafe-inline'",
            "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
            "font-src 'self' https://fonts.gstatic.com",
            "img-src 'self' data:",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'");

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /** Guarda y recupera el SecurityContext (la identidad) desde la sesión HTTP. */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            SecurityContextRepository contextRepository) throws Exception {
        http
                .securityContext(ctx -> ctx.securityContextRepository(contextRepository))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // ----- Público: páginas, recursos estáticos y autenticación -----
                        .requestMatchers("/", "/index", "/productos", "/login", "/registro", "/error").permitAll()
                        .requestMatchers("/styles.css", "/common.js", "/favicon.ico", "/uploads/**").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/registro",
                                "/api/auth/me", "/api/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/productos", "/api/productos/*").permitAll()
                        // ----- Solo ADMIN -----
                        .requestMatchers("/admin/**", "/api/pedidos/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/productos", "/api/productos/upload").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/productos/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/productos/*").hasRole("ADMIN")
                        // ----- Solo CLIENTE -----
                        .requestMatchers("/carrito", "/mis-pedidos", "/api/carrito/**",
                                "/api/pedidos/confirmar").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/pedidos").hasRole("CLIENTE")
                        // ----- Cualquier usuario autenticado (la propiedad del pedido se valida en el controlador) -----
                        .requestMatchers("/pedido/*", "/api/pedidos/*").authenticated()
                        // ----- Todo lo demás, denegado -----
                        .anyRequest().denyAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(SecurityConfig::handleUnauthenticated)
                        .accessDeniedHandler(SecurityConfig::handleAccessDenied))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CSP))
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(ref -> ref.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31_536_000)));
        return http.build();
    }

    // ------------------------------------------------------------------ manejo de 401 / 403

    private static boolean isApi(HttpServletRequest request) {
        return request.getRequestURI().startsWith(request.getContextPath() + "/api/");
    }

    /** Sin sesión: JSON 401 para la API, redirección al login para las páginas. */
    private static void handleUnauthenticated(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException ex) throws IOException {
        if (isApi(request)) {
            writeJson(request, response, HttpStatus.UNAUTHORIZED, "Debes iniciar sesión");
        } else {
            response.sendRedirect(request.getContextPath() + "/login");
        }
    }

    /** Sin permisos (o CSRF inválido): JSON 403 para la API, redirección según el rol para las páginas. */
    private static void handleAccessDenied(HttpServletRequest request, HttpServletResponse response,
                                           AccessDeniedException ex) throws IOException {
        if (isApi(request)) {
            String message = (ex instanceof CsrfException)
                    ? "Token CSRF inválido o ausente"
                    : "No tienes permisos para esta operación";
            writeJson(request, response, HttpStatus.FORBIDDEN, message);
            return;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean admin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        response.sendRedirect(request.getContextPath() + (admin ? "/admin/productos" : "/"));
    }

    private static void writeJson(HttpServletRequest request, HttpServletResponse response,
                                  HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getRequestURI());
        JSON.writeValue(response.getWriter(), body);
    }

    /** Fuerza a que la cookie XSRF-TOKEN se emita en cada respuesta para que el JS pueda leerla. */
    private static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (token != null) {
                token.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }
}
