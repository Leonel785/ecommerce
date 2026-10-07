package com.ecommerce.controller;

import com.ecommerce.entity.Rol;
import com.ecommerce.security.AuthUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Controlador de vistas Thymeleaf.
 * <p>
 * El control de acceso a las páginas (sesión iniciada, rol ADMIN o CLIENTE) lo resuelve
 * {@code SecurityConfig}: sin sesión se redirige a {@code /login}; con un rol incorrecto se
 * redirige a la página de inicio de ese rol. Este controlador solo elige la plantilla.
 * </p>
 */
@Controller
public class ViewController {

    /** Página de login. Si el usuario ya tiene sesión, lo envía a su página principal. */
    @GetMapping("/login")
    public String login(@AuthenticationPrincipal AuthUser user) {
        if (user != null) {
            return user.rol() == Rol.ADMIN ? "redirect:/admin/productos" : "redirect:/";
        }
        return "login";
    }

    /** Página de registro. Si el usuario ya tiene sesión, redirige al inicio. */
    @GetMapping("/registro")
    public String registro(@AuthenticationPrincipal AuthUser user) {
        if (user != null) {
            return "redirect:/";
        }
        return "registro";
    }

    /** Catálogo de productos (público). */
    @GetMapping({"/", "/index", "/productos"})
    public String index() {
        return "index";
    }

    /** Carrito de compras (rol CLIENTE). */
    @GetMapping("/carrito")
    public String carrito() {
        return "carrito";
    }

    /** Panel de administración de productos y pedidos (rol ADMIN). */
    @GetMapping("/admin/productos")
    public String adminProductos() {
        return "admin-productos";
    }

    /** Historial de pedidos del cliente (rol CLIENTE). */
    @GetMapping("/mis-pedidos")
    public String misPedidos() {
        return "mis-pedidos";
    }

    /** Detalle de un pedido (usuario autenticado; la propiedad se valida en la API). */
    @GetMapping("/pedido/{id}")
    public String pedidoDetalle(@PathVariable Long id, Model model) {
        model.addAttribute("pedidoId", id);
        return "pedido-detalle";
    }
}
