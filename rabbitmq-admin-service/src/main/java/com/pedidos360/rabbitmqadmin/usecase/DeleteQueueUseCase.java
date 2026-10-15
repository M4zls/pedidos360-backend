package com.pedidos360.rabbitmqadmin.usecase;

import org.springframework.stereotype.Service;

import com.pedidos360.rabbitmqadmin.broker.RabbitMqBroker;

@Service
public class DeleteQueueUseCase {

    private final RabbitMqBroker broker;

    public DeleteQueueUseCase(RabbitMqBroker broker) {
        this.broker = broker;
    }

    public void execute(String name) {
        broker.deleteQueue(name);
    }
}
