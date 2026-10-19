package com.pedidos360.rabbitmqadmin.broker;

import java.util.List;
import java.util.Map;

/**
 * Puerto hacia el broker RabbitMQ. Los servicios dependen de esta
 * abstraccion, nunca del cliente HTTP concreto.
 */
public interface RabbitMqBroker {

    List<ExchangeInfo> listExchanges();

    ExchangeInfo getExchange(String name);

    void createExchange(String name, String type, boolean durable, boolean autoDelete);

    void deleteExchange(String name);

    List<QueueInfo> listQueues();

    QueueInfo getQueue(String name);

    void createQueue(String name, boolean durable, boolean autoDelete, Map<String, Object> arguments);

    void deleteQueue(String name);

    List<BindingInfo> listBindings();

    void createBinding(String source, BindingDestinationType destinationType, String destination, String routingKey);

    void deleteBinding(String source, BindingDestinationType destinationType, String destination, String routingKey);
}
