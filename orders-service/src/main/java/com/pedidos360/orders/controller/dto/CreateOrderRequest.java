package com.pedidos360.orders.controller.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        @Size(max = 500) String note,
        @NotEmpty @Valid List<Line> lines
) {
    public record Line(
            @NotNull Long productId,
            @Positive int quantity
    ) {}
}
