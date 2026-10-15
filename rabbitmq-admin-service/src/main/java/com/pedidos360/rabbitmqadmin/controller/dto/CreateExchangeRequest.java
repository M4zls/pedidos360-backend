package com.pedidos360.rabbitmqadmin.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateExchangeRequest(
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9_.\\-]+") String name,
        @NotNull ExchangeType type,
        boolean durable,
        boolean autoDelete) {
}
