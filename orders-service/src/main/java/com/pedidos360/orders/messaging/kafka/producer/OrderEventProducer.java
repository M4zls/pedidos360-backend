package com.pedidos360.orders.messaging.kafka.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.pedidos360.orders.messaging.kafka.config.KafkaTopologyProperties;
import com.pedidos360.orders.messaging.kafka.event.OrderAnalyticsEvent;

/**
 * Productor Kafka de eventos de analitica de pedidos. Se llama ademas de (no
 * en lugar de) {@code RabbitEventPublisher}: RabbitMQ sigue resolviendo la
 * notificacion y el aviso a despacho, Kafka solo agrega el stream para
 * analitica/auditoria.
 */
@Component
public class OrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopologyProperties topology;

    public OrderEventProducer(KafkaTemplate<String, Object> kafkaTemplate, KafkaTopologyProperties topology) {
        this.kafkaTemplate = kafkaTemplate;
        this.topology = topology;
    }

    public void publish(OrderAnalyticsEvent event) {
        String topic = topology.topics().ordersEvents().name();
        kafkaTemplate.send(topic, event.orderId().toString(), event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("No se pudo publicar en Kafka ({}): orderId={} eventType={} -> {}",
                        topic, event.orderId(), event.eventType(), ex.getMessage(), ex);
            } else {
                log.info("Publicado en Kafka ({}): orderId={} eventType={}", topic, event.orderId(), event.eventType());
            }
        });
    }
}
