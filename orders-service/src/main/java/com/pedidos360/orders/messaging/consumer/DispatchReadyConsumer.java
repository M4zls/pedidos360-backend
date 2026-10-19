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
 * Consume {@code dispatch.ready}: avisa que un pedido quedo LISTO para que
 * despacho lo retire. Clase separada del productor y de
 * {@link OrderNotificationConsumer}, aunque ambas escuchen el mismo exchange.
 */
@Component
public class DispatchReadyConsumer {

    private static final Logger log = LoggerFactory.getLogger(DispatchReadyConsumer.class);

    private final EventValidator validator;

    public DispatchReadyConsumer(EventValidator validator) {
        this.validator = validator;
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.dispatch-ready.name}")
    public void onMessage(@Payload OrderStatusChangedEvent event, Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            validator.validate(event);
            log.info("Despacho: pedido #{} listo para retirar", event.orderId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("No se pudo avisar a despacho (pedido #{}): {} -> mensaje a DLQ",
                    event != null ? event.orderId() : "?", ex.getMessage(), ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
