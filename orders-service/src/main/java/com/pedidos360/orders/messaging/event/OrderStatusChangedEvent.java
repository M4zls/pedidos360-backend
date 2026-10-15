package com.pedidos360.orders.messaging.event;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Se publica al exchange topic {@code orders.status.topic} en cada cambio de estado. */
public record OrderStatusChangedEvent(
        @NotNull Long orderId,
        @NotBlank String customerEmail,
        @NotBlank String previousStatus,
        @NotBlank String newStatus,
        @NotNull Instant occurredAt) {
}
