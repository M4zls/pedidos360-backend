package com.pedidos360.rabbitmqadmin.controller.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

import com.pedidos360.rabbitmqadmin.controller.validation.RabbitResourceName;
import com.pedidos360.rabbitmqadmin.controller.validation.RabbitResourceRef;

/**
 * durable y autoDelete son opcionales: por defecto durable=true, autoDelete=false.
 *
 * @param deadLetterExchange   opcional; tiene que existir (lo chequea RabbitAdminService)
 * @param deadLetterRoutingKey opcional; solo tiene sentido junto con deadLetterExchange
 */
public record CreateQueueRequest(
        @RabbitResourceName String name,
        Boolean durable,
        Boolean autoDelete,
        @RabbitResourceRef String deadLetterExchange,
        @Size(max = RabbitResourceRef.MAX_LENGTH) String deadLetterRoutingKey) {

    public CreateQueueRequest {
        durable = durable == null || durable;
        autoDelete = autoDelete != null && autoDelete;
    }

    @AssertTrue(message = "deadLetterRoutingKey requiere deadLetterExchange")
    public boolean isDeadLetterRoutingKeyWithExchange() {
        return deadLetterRoutingKey == null || deadLetterExchange != null;
    }
}
