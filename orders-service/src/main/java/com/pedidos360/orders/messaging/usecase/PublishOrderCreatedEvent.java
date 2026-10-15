package com.pedidos360.orders.messaging.usecase;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.pedidos360.orders.domain.CustomerOrder;
import com.pedidos360.orders.messaging.event.OrderCreatedEvent;
import com.pedidos360.orders.messaging.event.OrderLineEvent;
import com.pedidos360.orders.messaging.publisher.EventPublisher;

@Service
public class PublishOrderCreatedEvent {

    private final EventPublisher publisher;

    public PublishOrderCreatedEvent(EventPublisher publisher) {
        this.publisher = publisher;
    }

    public void execute(CustomerOrder order) {
        List<OrderLineEvent> lines = order.getLines().stream()
                .map(l -> new OrderLineEvent(l.getProductId(), l.getQuantity()))
                .toList();
        publisher.publishOrderCreated(new OrderCreatedEvent(
                order.getId(), order.getCustomerEmail(), lines, Instant.now()));
    }
}
