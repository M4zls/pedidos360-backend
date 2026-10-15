package com.pedidos360.orders.messaging.event;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderLineEvent(
        @NotNull Long productId,
        @Positive int quantity) {
}
