package com.pedidos360.inventory.messaging.kafka.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.pedidos360.inventory.messaging.consumer.EventValidator;
import com.pedidos360.inventory.messaging.kafka.event.OrderAnalyticsEvent;

/**
 * Consume {@code orders.events} (consumer group {@code inventory-service-analytics}):
 * registra en el log los eventos de pedidos para cruzarlos despues con los
 * propios movimientos de stock en analitica. No dispara ninguna accion de
 * negocio — eso lo sigue resolviendo RabbitMQ (orders.direct); esto es solo
 * observabilidad adicional.
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final EventValidator validator;

    public OrderEventConsumer(EventValidator validator) {
        this.validator = validator;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.orders-events.name}",
            groupId = "${app.kafka.consumer.group-id}")
    public void onMessage(OrderAnalyticsEvent event) {
        validator.validate(event);
        log.info("[Kafka][analitica] Evento de pedido recibido: orderId={} eventType={} newStatus={}",
                event.orderId(), event.eventType(), event.newStatus());
    }
}
