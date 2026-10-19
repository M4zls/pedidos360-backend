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

import com.pedidos360.identity.messaging.event.UserEvent;

/**
 * Consume {@code users.audit}: deja registro de toda la actividad de usuarios
 * (altas, cambios de rol, consentimientos). En este alcance va al log; mas
 * adelante podria persistirse en una tabla de auditoria.
 */
@Component
public class UserAuditConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserAuditConsumer.class);

    private final EventValidator validator;

    public UserAuditConsumer(EventValidator validator) {
        this.validator = validator;
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.audit.name}")
    public void onMessage(@Payload UserEvent event, Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            validator.validate(event);
            if (event.previousRole() != null) {
                log.info("[auditoria] {} {}: {} -> {}",
                        event.email(), event.eventType(), event.previousRole(), event.role());
            } else {
                log.info("[auditoria] {} {} (rol {})", event.email(), event.eventType(), event.role());
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("No se pudo auditar el evento de {}: {} -> mensaje a DLQ",
                    event != null ? event.email() : "?", ex.getMessage(), ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
