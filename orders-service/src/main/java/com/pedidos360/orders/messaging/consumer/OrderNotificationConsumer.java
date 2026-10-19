package com.pedidos360.orders.messaging.consumer;

import java.io.IOException;

import com.rabbitmq.client.Channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.pedidos360.orders.messaging.event.OrderStatusChangedEvent;

/**
 * Consume {@code orders.notifications}: simula el aviso al cliente de que su
 * pedido cambio de estado (en este alcance, solo lo deja en el log; mas
 * adelante podria disparar un mail o un push). Clase separada del productor
 * y del {@code OrderService}: no conoce reglas de negocio, solo reacciona al
 * evento.
 */
@Component
public class OrderNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationConsumer.class);

    private final EventValidator validator;

    public OrderNotificationConsumer(EventValidator validator) {
        this.validator = validator;
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.orders-notifications.name}")
    public void onMessage(@Payload OrderStatusChangedEvent event, Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            validator.validate(event);
            log.info("Notificando a {}: pedido #{} ahora esta {}",
                    event.customerEmail(), event.orderId(), event.newStatus());
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("No se pudo notificar al cliente (pedido #{}): {} -> mensaje a DLQ",
                    event != null ? event.orderId() : "?", ex.getMessage(), ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
