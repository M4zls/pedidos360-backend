package com.pedidos360.orders.messaging.usecase;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.pedidos360.orders.domain.CustomerOrder;
import com.pedidos360.orders.messaging.event.OrderCreatedEvent;
import com.pedidos360.orders.messaging.event.OrderLineEvent;
import com.pedidos360.orders.messaging.kafka.event.OrderAnalyticsEvent;
import com.pedidos360.orders.messaging.kafka.producer.OrderEventProducer;
import com.pedidos360.orders.messaging.publisher.EventPublisher;

@Service
public class PublishOrderCreatedEvent {

    private final EventPublisher publisher;
    private final OrderEventProducer kafkaProducer;

    public PublishOrderCreatedEvent(EventPublisher publisher, OrderEventProducer kafkaProducer) {
        this.publisher = publisher;
        this.kafkaProducer = kafkaProducer;
    }

    public void execute(CustomerOrder order) {
        List<OrderLineEvent> lines = order.getLines().stream()
                .map(l -> new OrderLineEvent(l.getProductId(), l.getQuantity()))
                .toList();
        publisher.publishOrderCreated(new OrderCreatedEvent(
                order.getId(), order.getCustomerEmail(), lines, Instant.now()));

        // Kafka complementa a RabbitMQ (arriba): mismo hecho, stream de
        // analitica aparte, sin afectar la cola ni el consumidor transaccional.
        kafkaProducer.publish(new OrderAnalyticsEvent(
                order.getId(), order.getCustomerEmail(), "CREATED", null, order.getStatus().name(), Instant.now()));
    }
}
