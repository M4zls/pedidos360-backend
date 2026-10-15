package com.pedidos360.rabbitmqadmin.usecase;

import org.springframework.stereotype.Service;

import com.pedidos360.rabbitmqadmin.broker.RabbitMqBroker;

@Service
public class CreateExchangeUseCase {

    private final RabbitMqBroker broker;

    public CreateExchangeUseCase(RabbitMqBroker broker) {
        this.broker = broker;
    }

    public void execute(String name, String type, boolean durable, boolean autoDelete) {
        broker.createExchange(name, type, durable, autoDelete);
    }
}
