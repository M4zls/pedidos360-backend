package com.pedidos360.orders.messaging.kafka.event;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Evento de analitica/auditoria publicado al topico Kafka {@code orders.events}.
 * Independiente de {@code OrderCreatedEvent}/{@code OrderStatusChangedEvent}
 * (los eventos de RabbitMQ): Kafka complementa la arquitectura para streaming,
 * no reemplaza ni reutiliza el contrato transaccional existente.
 *
 * @param eventType       {@code CREATED} o {@code STATUS_CHANGED}
 * @param previousStatus  solo para {@code STATUS_CHANGED}; null en {@code CREATED}
 */
public record OrderAnalyticsEvent(
        @NotNull Long orderId,
        @NotBlank String customerEmail,
        @NotBlank String eventType,
        String previousStatus,
        @NotBlank String newStatus,
        @NotNull Instant occurredAt) {
}
