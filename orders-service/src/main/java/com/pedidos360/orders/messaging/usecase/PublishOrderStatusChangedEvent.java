package com.pedidos360.orders.messaging.usecase;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.pedidos360.orders.domain.CustomerOrder;
import com.pedidos360.orders.domain.OrderStatus;
import com.pedidos360.orders.messaging.event.OrderStatusChangedEvent;
import com.pedidos360.orders.messaging.kafka.event.OrderAnalyticsEvent;
import com.pedidos360.orders.messaging.kafka.producer.OrderEventProducer;
import com.pedidos360.orders.messaging.publisher.EventPublisher;

@Service
public class PublishOrderStatusChangedEvent {

    private final EventPublisher publisher;
    private final OrderEventProducer kafkaProducer;

    public PublishOrderStatusChangedEvent(EventPublisher publisher, OrderEventProducer kafkaProducer) {
        this.publisher = publisher;
        this.kafkaProducer = kafkaProducer;
    }

    public void execute(CustomerOrder order, OrderStatus previousStatus) {
        publisher.publishOrderStatusChanged(new OrderStatusChangedEvent(
                order.getId(),
                order.getCustomerEmail(),
                previousStatus.name(),
                order.getStatus().name(),
                Instant.now()));

        // Kafka complementa a RabbitMQ (arriba): mismo hecho, stream de
        // analitica aparte, sin afectar la cola ni el consumidor transaccional.
        kafkaProducer.publish(new OrderAnalyticsEvent(
                order.getId(), order.getCustomerEmail(), "STATUS_CHANGED",
                previousStatus.name(), order.getStatus().name(), Instant.now()));
    }
}
