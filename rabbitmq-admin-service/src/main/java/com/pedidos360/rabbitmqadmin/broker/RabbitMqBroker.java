package com.pedidos360.rabbitmqadmin.broker;

import java.util.Map;

/**
 * Puerto hacia el broker RabbitMQ. Los casos de uso dependen de esta
 * abstraccion, nunca del cliente HTTP concreto.
 */
public interface RabbitMqBroker {

    void createExchange(String name, String type, boolean durable, boolean autoDelete);

    void deleteExchange(String name);

    void createQueue(String name, boolean durable, boolean autoDelete, Map<String, Object> arguments);

    void deleteQueue(String name);

    void createBinding(String source, BindingDestinationType destinationType, String destination, String routingKey);

    void deleteBinding(String source, BindingDestinationType destinationType, String destination, String routingKey);
}
