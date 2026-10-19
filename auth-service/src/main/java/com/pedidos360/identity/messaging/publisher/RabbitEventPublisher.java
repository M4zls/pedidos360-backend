package com.pedidos360.identity.messaging.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.pedidos360.identity.messaging.config.RabbitTopologyProperties;
import com.pedidos360.identity.messaging.event.UserEvent;

@Component
public class RabbitEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final RabbitTopologyProperties topology;

    public RabbitEventPublisher(RabbitTemplate rabbitTemplate, RabbitTopologyProperties topology) {
        this.rabbitTemplate = rabbitTemplate;
        this.topology = topology;
    }

    /**
     * Si el broker no responde, se loguea y se sigue: el login (GET /api/me)
     * no puede fallar porque RabbitMQ este caido.
     */
    @Override
    public void publishUserEvent(UserEvent event) {
        String routingKey = topology.userRoutingKey(event.eventType(), event.role());
        try {
            rabbitTemplate.convertAndSend(topology.exchanges().usersTopic(), routingKey, event);
            log.info("Publicado {}: {}", routingKey, event.email());
        } catch (AmqpException ex) {
            log.error("No se pudo publicar {} para {}: {}", routingKey, event.email(), ex.getMessage());
        }
    }
}
