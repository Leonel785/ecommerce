package com.ecommerce.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Controlador MVC responsable de servir las vistas Thymeleaf del lado servidor.
 * <p>
 * Realiza las redirecciones pertinentes según el estado de la sesión y el rol
 * del usuario, evitando que un usuario autenticado vuelva a las pantallas de
 * login/registro y restringiendo el panel de administración a los administradores.
 * </p>
 */
@Controller
public class ViewController {

    /**
     * Muestra el formulario de inicio de sesión. Si el usuario ya está
     * autenticado, se redirige al panel correspondiente según su rol.
     *
     * @param session sesión HTTP actual.
     * @return nombre de la vista {@code login} o redirección.
     */
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

    /**
     * Muestra el formulario de registro. Si el usuario ya está autenticado,
     * se redirige a la página principal.
     *
     * @param session sesión HTTP actual.
     * @return nombre de la vista {@code registro} o redirección.
     */
    @GetMapping("/registro")
    public String registro(HttpSession session) {
        if (session.getAttribute("usuarioId") != null) {
            return "redirect:/";
        }
        return "registro";
    }

    /**
     * Sirve la página principal con el catálogo de productos.
     * Mapea las rutas raíz, {@code /index} y {@code /productos} a la misma vista.
     *
     * @param session sesión HTTP actual.
     * @return nombre de la vista {@code index}.
     */
    @GetMapping({"/", "/index", "/productos"})
    public String index(HttpSession session) {
        return "index";
    }

    /**
     * Muestra el carrito de compras del cliente. Requiere sesión iniciada
     * y rol {@code CLIENTE}; los administradores son redirigidos a su panel.
     *
     * @param session sesión HTTP actual.
     * @return nombre de la vista {@code carrito} o redirección.
     */
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

    /**
     * Muestra el panel de administración de productos. Acceso restringido
     * exclusivamente a usuarios con rol {@code ADMIN}.
     *
     * @param session sesión HTTP actual.
     * @return nombre de la vista {@code admin-productos} o redirección.
     */
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

    /**
     * Muestra el listado de pedidos del cliente autenticado. Requiere rol
     * {@code CLIENTE}; los administradores son redirigidos a su panel.
     *
     * @param session sesión HTTP actual.
     * @return nombre de la vista {@code mis-pedidos} o redirección.
     */
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

    /**
     * Muestra el detalle de un pedido específico. Requiere sesión iniciada.
     * <p>La verificación de propiedad del pedido (cliente dueño vs administrador)
     * se delega al endpoint REST {@code /api/pedidos/{id}} consumido por la vista.</p>
     *
     * @param id      identificador del pedido.
     * @param session sesión HTTP actual.
     * @param model   modelo de Thymeleaf donde se añade el id del pedido.
     * @return nombre de la vista {@code pedido-detalle} o redirección.
     */
    @GetMapping("/pedido/{id}")
    public String pedidoDetalle(@PathVariable Long id, HttpSession session, Model model) {
        if (session.getAttribute("usuarioId") == null) {
            return "redirect:/login";
        }
        model.addAttribute("pedidoId", id);
        return "pedido-detalle";
    }
}
