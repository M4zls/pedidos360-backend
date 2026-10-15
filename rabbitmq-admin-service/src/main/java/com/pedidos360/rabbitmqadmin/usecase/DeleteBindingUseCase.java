package com.pedidos360.rabbitmqadmin.usecase;

import org.springframework.stereotype.Service;

import com.pedidos360.rabbitmqadmin.broker.BindingDestinationType;
import com.pedidos360.rabbitmqadmin.broker.RabbitMqBroker;

@Service
public class DeleteBindingUseCase {

    private final RabbitMqBroker broker;

    public DeleteBindingUseCase(RabbitMqBroker broker) {
        this.broker = broker;
    }

    public void execute(String source, BindingDestinationType destinationType, String destination, String routingKey) {
        broker.deleteBinding(source, destinationType, destination, routingKey);
    }
}
