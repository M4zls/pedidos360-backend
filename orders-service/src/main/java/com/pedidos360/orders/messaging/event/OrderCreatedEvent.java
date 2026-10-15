package com.pedidos360.orders.messaging.event;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** Se publica una vez al exchange direct {@code orders.direct}, al crear el pedido. */
public record OrderCreatedEvent(
        @NotNull Long orderId,
        @NotBlank String customerEmail,
        @NotEmpty @Valid List<OrderLineEvent> lines,
        @NotNull Instant occurredAt) {
}
