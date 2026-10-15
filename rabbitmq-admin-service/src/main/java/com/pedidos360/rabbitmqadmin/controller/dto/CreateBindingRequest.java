package com.pedidos360.rabbitmqadmin.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.pedidos360.rabbitmqadmin.broker.BindingDestinationType;

public record CreateBindingRequest(
        @NotBlank String source,
        @NotNull BindingDestinationType destinationType,
        @NotBlank String destination,
        String routingKey) {
}
