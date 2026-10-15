package com.pedidos360.inventory.messaging;

/**
 * Nombres de exchanges, colas y routing keys que le importan a inventory-service.
 * El exchange {@code orders.direct} lo declara tambien orders-service (su
 * productor); declararlo aca de nuevo es idempotente mientras coincidan sus
 * propiedades (nombre, tipo, durable).
 */
public final class RabbitTopology {

    private RabbitTopology() {
    }

    public static final String ORDERS_DIRECT_EXCHANGE = "orders.direct";
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";

    public static final String INVENTORY_STOCK_RESERVE_QUEUE = "inventory.stock-reserve";
    public static final String INVENTORY_STOCK_RESERVE_DLX = "inventory.stock-reserve.dlx";
    public static final String INVENTORY_STOCK_RESERVE_DLQ = "inventory.stock-reserve.dlq";
}
