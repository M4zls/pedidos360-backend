package com.pedidos360.orders.messaging.kafka.event;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Evento de analitica publicado por inventory-service al topico Kafka
 * {@code inventory.events}. Copia del contrato (mismo criterio que los
 * eventos de RabbitMQ duplicados entre servicios): cada servicio es dueño de
 * su propia copia, no de una libreria compartida.
 */
public record InventoryAnalyticsEvent(
        @NotNull Long orderId,
        @NotNull Long productId,
        @NotBlank String eventType,
        @Positive int quantity,
        @NotNull Instant occurredAt) {
}
