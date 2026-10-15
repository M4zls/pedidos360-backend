package com.pedidos360.orders.messaging.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.pedidos360.orders.messaging.event.OrderCreatedEvent;
import com.pedidos360.orders.messaging.event.OrderStatusChangedEvent;

import static com.pedidos360.orders.messaging.RabbitTopology.*;

@Component
public class RabbitEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishOrderCreated(OrderCreatedEvent event) {
        rabbitTemplate.convertAndSend(ORDERS_DIRECT_EXCHANGE, ORDER_CREATED_ROUTING_KEY, event,
                message -> {
                    message.getMessageProperties().setHeader("__TypeId__", "orderCreatedEvent");
                    return message;
                });
        log.info("Publicado order.created: orderId={}", event.orderId());
    }

    @Override
    public void publishOrderStatusChanged(OrderStatusChangedEvent event) {
        String routingKey = statusRoutingKey(event.newStatus());
        rabbitTemplate.convertAndSend(ORDERS_STATUS_TOPIC_EXCHANGE, routingKey, event);
        log.info("Publicado {}: orderId={} {} -> {}",
                routingKey, event.orderId(), event.previousStatus(), event.newStatus());
    }
}
