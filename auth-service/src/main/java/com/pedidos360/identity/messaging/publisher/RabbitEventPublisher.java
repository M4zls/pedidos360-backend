package com.pedidos360.identity.messaging.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.pedidos360.identity.messaging.event.UserEvent;

import static com.pedidos360.identity.messaging.RabbitTopology.*;

@Component
public class RabbitEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Si el broker no responde, se loguea y se sigue: el login (GET /api/me)
     * no puede fallar porque RabbitMQ este caido.
     */
    @Override
    public void publishUserEvent(UserEvent event) {
        String routingKey = userRoutingKey(event.eventType(), event.role());
        try {
            rabbitTemplate.convertAndSend(USERS_TOPIC_EXCHANGE, routingKey, event);
            log.info("Publicado {}: {}", routingKey, event.email());
        } catch (AmqpException ex) {
            log.error("No se pudo publicar {} para {}: {}", routingKey, event.email(), ex.getMessage());
        }
    }
}
