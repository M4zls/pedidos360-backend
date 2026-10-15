package com.pedidos360.rabbitmqadmin.usecase;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.pedidos360.rabbitmqadmin.broker.RabbitMqBroker;

@Service
public class CreateQueueUseCase {

    private final RabbitMqBroker broker;

    public CreateQueueUseCase(RabbitMqBroker broker) {
        this.broker = broker;
    }

    /**
     * Crea una cola. Si se indica una dead-letter-exchange, los mensajes
     * rechazados o no entregados (nack sin requeue, TTL vencido) van a parar
     * ahi en vez de perderse.
     */
    public void execute(String name, boolean durable, boolean autoDelete,
            String deadLetterExchange, String deadLetterRoutingKey) {
        Map<String, Object> arguments = new HashMap<>();
        if (deadLetterExchange != null && !deadLetterExchange.isBlank()) {
            arguments.put("x-dead-letter-exchange", deadLetterExchange);
            if (deadLetterRoutingKey != null && !deadLetterRoutingKey.isBlank()) {
                arguments.put("x-dead-letter-routing-key", deadLetterRoutingKey);
            }
        }
        broker.createQueue(name, durable, autoDelete, arguments);
    }
}
