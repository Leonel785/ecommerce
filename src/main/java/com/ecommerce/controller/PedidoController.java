package com.ecommerce.controller;

import com.ecommerce.dto.PedidoResponse;
import com.ecommerce.entity.Cliente;
import com.ecommerce.entity.EstadoPedido;
import com.ecommerce.entity.Pedido;
import com.ecommerce.exception.ApiException;
import com.ecommerce.service.PedidoService;
import com.ecommerce.util.SessionGuard;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la gestión de pedidos.
 * <p>
 * Permite confirmar un pedido a partir del carrito, listar pedidos del cliente
 * autenticado, consultar un pedido específico, listar todos los pedidos (admin)
 * y actualizar el estado de un pedido (admin).
 * </p>
 *
 * <p>Endpoints disponibles:</p>
 * <ul>
 *   <li>{@code POST /api/pedidos/confirmar}            — Convierte el carrito en pedido.</li>
 *   <li>{@code GET  /api/pedidos}                      — Lista los pedidos del cliente.</li>
 *   <li>{@code GET  /api/pedidos/{id}}                 — Obtiene un pedido por id.</li>
 *   <li>{@code GET  /api/pedidos/admin}                — Lista todos los pedidos (admin).</li>
 *   <li>{@code PUT  /api/pedidos/admin/{id}/estado}    — Cambia el estado (admin).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    /**
     * Convierte el carrito del cliente autenticado en un pedido.
     * <p>Verifica que el carrito no esté vacío, valida el stock de cada producto,
     * descuenta las cantidades del inventario, genera el {@code Pedido} con sus
     * detalles y vacía el carrito.</p>
     *
     * @param session sesión HTTP del cliente autenticado.
     * @return pedido confirmado.
     */
    @PostMapping("/confirmar")
    public PedidoResponse confirm(HttpSession session) {
        return PedidoResponse.from(pedidoService.confirm(SessionGuard.requireClient(session)));
    }

    /**
     * Lista todos los pedidos del cliente autenticado, ordenados del más
     * reciente al más antiguo.
     *
     * @param session sesión HTTP del cliente.
     * @return lista de pedidos del cliente.
     */
    @GetMapping
    public List<PedidoResponse> all(HttpSession session) {
        return pedidoService.findFor(SessionGuard.requireClient(session)).stream()
                .map(PedidoResponse::from).toList();
    }

    /**
     * Obtiene un pedido por su identificador.
     * <p>Aplica reglas de autorización: los clientes sólo pueden ver sus
     * propios pedidos, los administradores pueden ver cualquiera.</p>
     *
     * @param id      identificador del pedido.
     * @param session sesión HTTP del usuario.
     * @return pedido solicitado.
     * @throws ApiException si la sesión no es válida, el cliente no es dueño
     *                     del pedido o el rol no tiene permisos.
     */
    @GetMapping("/{id}")
    public PedidoResponse getById(@PathVariable Long id, HttpSession session) {
        if (session.getAttribute("usuarioId") == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión");
        }
        String rol = (String) session.getAttribute("rol");
        Pedido pedido = pedidoService.findById(id);

        if ("CLIENTE".equals(rol)) {
            Cliente cliente = SessionGuard.requireClient(session);
            if (!pedido.getCliente().getId().equals(cliente.getId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "No tienes permisos para ver este pedido");
            }
        } else if (!"ADMIN".equals(rol)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No tienes permisos para esta operación");
        }

        return PedidoResponse.from(pedido);
    }

    /**
     * Lista todos los pedidos del sistema. Acceso exclusivo para usuarios
     * con rol {@code ADMIN}.
     *
     * @param session sesión HTTP del administrador.
     * @return lista completa de pedidos.
     */
    @GetMapping("/admin")
    public List<PedidoResponse> adminAll(HttpSession session) {
        SessionGuard.requireAdmin(session);
        return pedidoService.findAll().stream()
                .map(PedidoResponse::from).toList();
    }

    /**
     * Actualiza el estado de un pedido (por ejemplo, marcar como
     * {@code PAGADO}, {@code ENVIADO} o {@code ENTREGADO}).
     * <p>Endpoint restringido a administradores.</p>
     *
     * @param id      identificador del pedido.
     * @param estado  nuevo estado a aplicar (provisto como query param).
     * @param session sesión HTTP del administrador.
     * @return pedido con el estado actualizado.
     */
    @PutMapping("/admin/{id}/estado")
    public PedidoResponse updateEstado(@PathVariable Long id, @RequestParam EstadoPedido estado, HttpSession session) {
        SessionGuard.requireAdmin(session);
        return PedidoResponse.from(pedidoService.updateEstado(id, estado));
    }
}