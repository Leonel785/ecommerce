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

    @PostMapping("/registro")
    public UsuarioResponse register(@Valid @RequestBody RegistroRequest request, HttpSession session) {
        Cliente cliente = usuarioService.register(request);
        Usuario user = cliente.getUsuario();
        session.setAttribute("usuarioId", user.getId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("rol", user.getRol().name());
        return new UsuarioResponse(user.getId(), user.getUsername(), user.getRol().name(), cliente.getNombres());
    }

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

    @PostMapping("/logout")
    public Map<String, String> logout(HttpSession session) {
        session.invalidate();
        return Map.of("message", "Sesión cerrada");
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
}