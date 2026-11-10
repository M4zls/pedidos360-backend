package com.pedidos360.inventory.messaging.kafka.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.pedidos360.inventory.messaging.kafka.config.KafkaTopologyProperties;
import com.pedidos360.inventory.messaging.kafka.event.InventoryAnalyticsEvent;

/**
 * Productor Kafka de eventos de analitica de inventario. Se llama ademas de
 * (no en lugar de) la reserva de stock que ya hace {@code InventoryService}
 * dentro del consumo de RabbitMQ: Kafka solo agrega el stream para
 * analitica/auditoria.
 */
@Component
public class InventoryEventProducer {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopologyProperties topology;

    public InventoryEventProducer(KafkaTemplate<String, Object> kafkaTemplate, KafkaTopologyProperties topology) {
        this.kafkaTemplate = kafkaTemplate;
        this.topology = topology;
    }

    public void publish(InventoryAnalyticsEvent event) {
        String topic = topology.topics().inventoryEvents().name();
        kafkaTemplate.send(topic, event.productId().toString(), event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("No se pudo publicar en Kafka ({}): productId={} orderId={} -> {}",
                        topic, event.productId(), event.orderId(), ex.getMessage(), ex);
            } else {
                log.info("Publicado en Kafka ({}): productId={} orderId={} eventType={}",
                        topic, event.productId(), event.orderId(), event.eventType());
            }
        });
    }
}
