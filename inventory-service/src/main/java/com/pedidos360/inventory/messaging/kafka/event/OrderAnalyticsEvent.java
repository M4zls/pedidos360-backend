package com.pedidos360.inventory.messaging.kafka.event;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Evento de analitica publicado por orders-service al topico Kafka
 * {@code orders.events}. Copia del contrato (mismo criterio que los eventos
 * de RabbitMQ duplicados entre servicios): cada servicio es dueño de su
 * propia copia, no de una libreria compartida.
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
