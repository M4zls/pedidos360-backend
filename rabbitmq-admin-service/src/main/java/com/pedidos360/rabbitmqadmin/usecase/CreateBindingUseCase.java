package com.pedidos360.rabbitmqadmin.usecase;

import org.springframework.stereotype.Service;

import com.pedidos360.rabbitmqadmin.broker.BindingDestinationType;
import com.pedidos360.rabbitmqadmin.broker.RabbitMqBroker;

@Service
public class CreateBindingUseCase {

    private final RabbitMqBroker broker;

    public CreateBindingUseCase(RabbitMqBroker broker) {
        this.broker = broker;
    }

    public void execute(String source, BindingDestinationType destinationType, String destination, String routingKey) {
        broker.createBinding(source, destinationType, destination, routingKey);
    }
}
