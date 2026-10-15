package com.pedidos360.orders.messaging.publisher;

import com.pedidos360.orders.messaging.event.OrderCreatedEvent;
import com.pedidos360.orders.messaging.event.OrderStatusChangedEvent;

/**
 * Puerto hacia el broker de mensajeria. Los casos de uso dependen de esta
 * abstraccion, nunca de RabbitTemplate directamente.
 */
public interface EventPublisher {

    void publishOrderCreated(OrderCreatedEvent event);

    void publishOrderStatusChanged(OrderStatusChangedEvent event);
}
