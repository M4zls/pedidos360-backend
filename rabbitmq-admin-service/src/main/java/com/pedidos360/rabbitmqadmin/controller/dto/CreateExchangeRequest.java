package com.pedidos360.rabbitmqadmin.controller.dto;

import jakarta.validation.constraints.NotNull;

import com.pedidos360.rabbitmqadmin.controller.validation.RabbitResourceName;

/** durable y autoDelete son opcionales: por defecto durable=true, autoDelete=false. */
public record CreateExchangeRequest(
        @RabbitResourceName String name,
        @NotNull ExchangeType type,
        Boolean durable,
        Boolean autoDelete) {

    public CreateExchangeRequest {
        durable = durable == null || durable;
        autoDelete = autoDelete != null && autoDelete;
    }
}
