package com.pedidos360.orders.messaging.kafka.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.pedidos360.orders.messaging.consumer.EventValidator;
import com.pedidos360.orders.messaging.kafka.event.InventoryAnalyticsEvent;

/**
 * Consume {@code inventory.events} (consumer group {@code orders-service-analytics}):
 * registra en el log los movimientos de stock para cruzarlos despues con los
 * propios pedidos en analitica. No dispara ninguna accion de negocio — eso lo
 * sigue resolviendo RabbitMQ (inventory.stock-reserve); esto es solo
 * observabilidad adicional.
 */
@Component
public class InventoryEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventConsumer.class);

    private final EventValidator validator;

    public InventoryEventConsumer(EventValidator validator) {
        this.validator = validator;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.inventory-events.name}",
            groupId = "${app.kafka.consumer.group-id}")
    public void onMessage(InventoryAnalyticsEvent event) {
        validator.validate(event);
        log.info("[Kafka][analitica] Stock reservado: productId={} quantity={} orderId={}",
                event.productId(), event.quantity(), event.orderId());
    }
}
