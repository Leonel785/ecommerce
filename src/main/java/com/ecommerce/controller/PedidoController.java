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

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping("/confirmar")
    public PedidoResponse confirm(HttpSession session) {
        return PedidoResponse.from(pedidoService.confirm(SessionGuard.requireClient(session)));
    }

    @GetMapping
    public List<PedidoResponse> all(HttpSession session) {
        return pedidoService.findFor(SessionGuard.requireClient(session)).stream()
                .map(PedidoResponse::from).toList();
    }

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

    @GetMapping("/admin")
    public List<PedidoResponse> adminAll(HttpSession session) {
        SessionGuard.requireAdmin(session);
        return pedidoService.findAll().stream()
                .map(PedidoResponse::from).toList();
    }

    @PutMapping("/admin/{id}/estado")
    public PedidoResponse updateEstado(@PathVariable Long id, @RequestParam EstadoPedido estado, HttpSession session) {
        SessionGuard.requireAdmin(session);
        return PedidoResponse.from(pedidoService.updateEstado(id, estado));
    }
}