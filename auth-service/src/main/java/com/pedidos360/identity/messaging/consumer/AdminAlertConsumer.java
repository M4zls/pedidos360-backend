package com.pedidos360.identity.messaging.consumer;

import java.io.IOException;

import com.rabbitmq.client.Channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.pedidos360.identity.domain.Role;
import com.pedidos360.identity.messaging.event.UserEvent;

/**
 * Consume {@code users.admin-alerts}: avisa cuando una cuenta entra o asciende
 * a ADMIN (el binding {@code user.*.admin} ya filtra). Es el evento sensible
 * de seguridad: alguien gano acceso total. En este alcance va al log.
 */
@Component
public class AdminAlertConsumer {

    private static final Logger log = LoggerFactory.getLogger(AdminAlertConsumer.class);

    private final EventValidator validator;

    public AdminAlertConsumer(EventValidator validator) {
        this.validator = validator;
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.admin-alerts.name}")
    public void onMessage(@Payload UserEvent event, Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            validator.validate(event);
            if (!Role.ADMIN.name().equals(event.role())) {
                throw new MessageProcessingException("Evento sin rol ADMIN en la cola de alertas: " + event.role());
            }
            log.warn("[alerta ADMIN] {} ({}): {}", event.email(), event.name(), event.eventType());
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("No se pudo procesar la alerta de {}: {} -> mensaje a DLQ",
                    event != null ? event.email() : "?", ex.getMessage(), ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
