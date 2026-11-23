package com.pedidos360.analytics.messaging.kafka.event;

import java.time.Instant;

/**
 * Copia del contrato que publica orders-service en {@code orders.events}.
 * Mismo criterio que las copias de evento entre orders/inventory-service:
 * cada servicio es dueño de su propia copia, no de una libreria compartida.
 *
 * @param eventType       {@code CREATED} o {@code STATUS_CHANGED}
 * @param previousStatus  solo para {@code STATUS_CHANGED}; null en {@code CREATED}
 */
public record OrderAnalyticsEvent(
        Long orderId,
        String customerEmail,
        String eventType,
        String previousStatus,
        String newStatus,
        Instant occurredAt) {
}
