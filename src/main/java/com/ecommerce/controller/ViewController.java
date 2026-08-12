package com.ecommerce.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ViewController {

    @GetMapping("/login")
    public String login(HttpSession session) {
        if (session.getAttribute("usuarioId") != null) {
            String rol = (String) session.getAttribute("rol");
            if ("ADMIN".equals(rol)) {
                return "redirect:/admin/productos";
            }
            return "redirect:/";
        }
        return "login";
    }

    @GetMapping("/registro")
    public String registro(HttpSession session) {
        if (session.getAttribute("usuarioId") != null) {
            return "redirect:/";
        }
        return "registro";
    }

    @GetMapping({"/", "/index", "/productos"})
    public String index(HttpSession session) {
        return "index";
    }

    @GetMapping("/carrito")
    public String carrito(HttpSession session) {
        if (session.getAttribute("usuarioId") == null) {
            return "redirect:/login";
        }
        String rol = (String) session.getAttribute("rol");
        if ("ADMIN".equals(rol)) {
            return "redirect:/admin/productos";
        }
        return "carrito";
    }

    @GetMapping("/admin/productos")
    public String adminProductos(HttpSession session) {
        if (session.getAttribute("usuarioId") == null) {
            return "redirect:/login";
        }
        String rol = (String) session.getAttribute("rol");
        if (!"ADMIN".equals(rol)) {
            return "redirect:/";
        }
        return "admin-productos";
    }

    @GetMapping("/mis-pedidos")
    public String misPedidos(HttpSession session) {
        if (session.getAttribute("usuarioId") == null) {
            return "redirect:/login";
        }
        String rol = (String) session.getAttribute("rol");
        if ("ADMIN".equals(rol)) {
            return "redirect:/admin/productos";
        }
        return "mis-pedidos";
    }

    @GetMapping("/pedido/{id}")
    public String pedidoDetalle(@PathVariable Long id, HttpSession session, Model model) {
        if (session.getAttribute("usuarioId") == null) {
            return "redirect:/login";
        }
        model.addAttribute("pedidoId", id);
        return "pedido-detalle";
    }
}
