package com.pedidos360.rabbitmqadmin.broker;

/** Un binding exchange -> cola/exchange tal como lo reporta el broker. */
public record BindingInfo(
        String source,
        BindingDestinationType destinationType,
        String destination,
        String routingKey) {
}
