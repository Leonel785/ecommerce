package com.ecommerce.dto;

import com.ecommerce.entity.Pedido;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Objeto de transferencia de datos (DTO) para la respuesta de un pedido realizado.
 *
 * @param id       identificador único del pedido.
 * @param fecha    fecha y hora en que se realizó o confirmó el pedido.
 * @param total    monto total pagado o a pagar por el pedido.
 * @param estado   estado actual del pedido (ej. PENDIENTE, PAGADO, ENVIADO, ENTREGADO).
 * @param detalles lista de ítems o productos comprados en el pedido.
 */
public record PedidoResponse(
        Long id, LocalDateTime fecha, BigDecimal total, String estado, List<DetallePedidoResponse> detalles
) {
    /**
     * Convierte una entidad {@link Pedido} a {@link PedidoResponse}.
     *
     * @param p entidad fuente.
     * @return DTO transformado.
     */
    public static PedidoResponse from(Pedido p) {
        return new PedidoResponse(p.getId(), p.getFecha(), p.getTotal(), p.getEstado().name(),
                p.getDetalles().stream().map(DetallePedidoResponse::from).toList());
    }
}