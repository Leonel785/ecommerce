package com.ecommerce.entity;

/**
 * Enumeración que representa el flujo de estados de un pedido en el sistema.
 */
public enum EstadoPedido {
    /** El pedido ha sido creado pero aún está en verificación. */
    PENDIENTE,
    /** El pedido ha sido pagado y confirmado por el cliente. */
    PAGADO,
    /** El pedido ha sido despachado y se encuentra en camino. */
    ENVIADO,
    /** El pedido ha sido entregado exitosamente al cliente. */
    ENTREGADO
}