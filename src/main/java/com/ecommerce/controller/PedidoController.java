package com.ecommerce.controller;

import com.ecommerce.dto.PedidoResponse;
import com.ecommerce.entity.Cliente;
import com.ecommerce.entity.EstadoPedido;
import com.ecommerce.entity.Pedido;
import com.ecommerce.exception.ApiException;
import com.ecommerce.service.PedidoService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.ecommerce.entity.Rol;
import com.ecommerce.security.AuthUser;
import com.ecommerce.security.CurrentClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

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
    private final CurrentClient currentClient;

    public PedidoController(PedidoService pedidoService, CurrentClient currentClient) {
        this.pedidoService = pedidoService;
        this.currentClient = currentClient;
    }

    /**
     * Convierte el carrito del cliente autenticado en un pedido.
     * <p>Verifica que el carrito no esté vacío, valida el stock de cada producto,
     * descuenta las cantidades del inventario, genera el {@code Pedido} con sus
     * detalles y vacía el carrito.</p>
     *
     * @return pedido confirmado.
     */
    @PreAuthorize("hasRole('CLIENTE')")
    @PostMapping("/confirmar")
    public PedidoResponse confirm(@AuthenticationPrincipal AuthUser user) {
        return PedidoResponse.from(pedidoService.confirm(currentClient.from(user)));
    }

    /**
     * Lista todos los pedidos del cliente autenticado, ordenados del más
     * reciente al más antiguo.
     *
     * @return lista de pedidos del cliente.
     */
    @PreAuthorize("hasRole('CLIENTE')")
    @GetMapping
    public List<PedidoResponse> all(@AuthenticationPrincipal AuthUser user) {
        return pedidoService.findFor(currentClient.from(user)).stream()
                .map(PedidoResponse::from).toList();
    }

    /**
     * Obtiene un pedido por su identificador.
     * <p>Aplica reglas de autorización: los clientes sólo pueden ver sus
     * propios pedidos, los administradores pueden ver cualquiera.</p>
     *
     * @param id      identificador del pedido.
     * @return pedido solicitado.
     * @throws ApiException si la sesión no es válida, el cliente no es dueño
     *                     del pedido o el rol no tiene permisos.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public PedidoResponse getById(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión");
        }
        Pedido pedido = pedidoService.findById(id);

        // El ADMIN puede ver cualquier pedido; el CLIENTE solo los suyos.
        if (user.rol() == Rol.CLIENTE) {
            Cliente cliente = currentClient.from(user);
            if (!pedido.getCliente().getId().equals(cliente.getId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "No tienes permisos para ver este pedido");
            }
        }

        return PedidoResponse.from(pedido);
    }

    /**
     * Lista todos los pedidos del sistema. Acceso exclusivo para usuarios
     * con rol {@code ADMIN}.
     *
     * @return lista completa de pedidos.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public List<PedidoResponse> adminAll() {
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
     * @return pedido con el estado actualizado.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/{id}/estado")
    public PedidoResponse updateEstado(@PathVariable Long id, @RequestParam EstadoPedido estado) {
        return PedidoResponse.from(pedidoService.updateEstado(id, estado));
    }
}