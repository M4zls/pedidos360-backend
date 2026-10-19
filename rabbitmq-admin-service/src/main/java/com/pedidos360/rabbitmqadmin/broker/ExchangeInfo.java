package com.pedidos360.rabbitmqadmin.broker;

/** Un exchange tal como lo reporta el broker. */
public record ExchangeInfo(
        String name,
        String type,
        boolean durable,
        boolean autoDelete) {
}
