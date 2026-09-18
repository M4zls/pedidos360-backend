package com.pedidos360.orders.domain;

/**
 * Estados de un pedido. Flujo normal:
 * PENDIENTE -> EN_PREPARACION -> LISTO -> DESPACHADO -> ENTREGADO.
 * Desde cualquier estado no terminal se puede pasar a CANCELADO.
 */
public enum OrderStatus {
    PENDIENTE,
    EN_PREPARACION,
    LISTO,
    DESPACHADO,
    ENTREGADO,
    CANCELADO;

    public boolean isTerminal() {
        return this == ENTREGADO || this == CANCELADO;
    }
}
