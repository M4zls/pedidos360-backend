package com.pedidos360.inventory.messaging.event;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Copia local del contrato que publica orders-service al exchange
 * {@code orders.direct}. Cada servicio es dueno de su propia vista del
 * mensaje (no comparten codigo entre si, igual que el resto del proyecto).
 */
public record OrderCreatedEvent(
        @NotNull Long orderId,
        @NotBlank String customerEmail,
        @NotEmpty @Valid List<OrderLineEvent> lines,
        @NotNull Instant occurredAt) {
}
