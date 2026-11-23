package com.pedidos360.analytics.messaging.kafka.event;

import java.time.Instant;

/**
 * Evento que PRODUCE este servicio al topico {@code order-fulfillment.events}:
 * el cruce entre "pedido creado" ({@code orders.events}) y "stock reservado
 * para ese pedido" ({@code inventory.events}), armado por
 * {@code OrderFulfillmentTopology}. Una linea de pedido = un evento (si el
 * pedido tiene varios productos, llega un {@code OrderFulfillmentEvent} por
 * cada uno).
 */
public record OrderFulfillmentEvent(
        Long orderId,
        String customerEmail,
        Long productId,
        int quantity,
        Instant orderCreatedAt,
        Instant stockReservedAt,
        long millisToReserve) {
}
