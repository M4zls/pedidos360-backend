package com.pedidos360.rabbitmqadmin.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.pedidos360.rabbitmqadmin.broker.BindingDestinationType;
import com.pedidos360.rabbitmqadmin.controller.validation.RabbitResourceRef;

/** El origen y el destino tienen que existir (lo chequea RabbitAdminService). */
public record CreateBindingRequest(
        @NotBlank @RabbitResourceRef String source,
        @NotNull BindingDestinationType destinationType,
        @NotBlank @RabbitResourceRef String destination,
        @Size(max = RabbitResourceRef.MAX_LENGTH) String routingKey) {
}
