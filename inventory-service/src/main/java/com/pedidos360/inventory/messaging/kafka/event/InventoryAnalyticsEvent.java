package com.pedidos360.inventory.messaging.kafka.event;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Evento de analitica/auditoria publicado al topico Kafka {@code inventory.events}.
 * Independiente del {@code OrderCreatedEvent} de RabbitMQ: Kafka complementa
 * la arquitectura para streaming, no reemplaza ni reutiliza el contrato
 * transaccional existente.
 */
public record InventoryAnalyticsEvent(
        @NotNull Long orderId,
        @NotNull Long productId,
        @NotBlank String eventType,
        @Positive int quantity,
        @NotNull Instant occurredAt) {
}
