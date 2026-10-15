package com.pedidos360.rabbitmqadmin.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateQueueRequest(
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9_.\\-]+") String name,
        boolean durable,
        boolean autoDelete,
        String deadLetterExchange,
        String deadLetterRoutingKey) {
}
