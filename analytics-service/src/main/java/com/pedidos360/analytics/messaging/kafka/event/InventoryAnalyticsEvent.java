package com.pedidos360.analytics.messaging.kafka.event;

import java.time.Instant;

/**
 * Copia del contrato que publica inventory-service en {@code inventory.events}.
 * Mismo criterio que {@link OrderAnalyticsEvent}: copia propia, no libreria
 * compartida.
 */
public record InventoryAnalyticsEvent(
        Long orderId,
        Long productId,
        String eventType,
        int quantity,
        Instant occurredAt) {
}
