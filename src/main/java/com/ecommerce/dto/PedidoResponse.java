package com.ecommerce.dto;

import com.ecommerce.entity.Pedido;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Long id, LocalDateTime fecha, BigDecimal total, String estado, List<DetallePedidoResponse> detalles
) {
    public static PedidoResponse from(Pedido p) {
        return new PedidoResponse(p.getId(), p.getFecha(), p.getTotal(), p.getEstado().name(),
                p.getDetalles().stream().map(DetallePedidoResponse::from).toList());
    }
}