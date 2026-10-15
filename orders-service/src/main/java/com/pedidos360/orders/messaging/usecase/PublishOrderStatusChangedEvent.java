package com.pedidos360.orders.messaging.usecase;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.pedidos360.orders.domain.CustomerOrder;
import com.pedidos360.orders.domain.OrderStatus;
import com.pedidos360.orders.messaging.event.OrderStatusChangedEvent;
import com.pedidos360.orders.messaging.publisher.EventPublisher;

@Service
public class PublishOrderStatusChangedEvent {

    private final EventPublisher publisher;

    public PublishOrderStatusChangedEvent(EventPublisher publisher) {
        this.publisher = publisher;
    }

    public void execute(CustomerOrder order, OrderStatus previousStatus) {
        publisher.publishOrderStatusChanged(new OrderStatusChangedEvent(
                order.getId(),
                order.getCustomerEmail(),
                previousStatus.name(),
                order.getStatus().name(),
                Instant.now()));
    }
}
