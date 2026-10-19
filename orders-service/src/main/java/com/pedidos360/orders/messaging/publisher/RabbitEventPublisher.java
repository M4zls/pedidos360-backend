package com.pedidos360.orders.messaging.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.pedidos360.orders.messaging.config.RabbitTopologyProperties;
import com.pedidos360.orders.messaging.event.OrderCreatedEvent;
import com.pedidos360.orders.messaging.event.OrderStatusChangedEvent;

@Component
public class RabbitEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final RabbitTopologyProperties topology;

    public RabbitEventPublisher(RabbitTemplate rabbitTemplate, RabbitTopologyProperties topology) {
        this.rabbitTemplate = rabbitTemplate;
        this.topology = topology;
    }

    @Override
    public void publishOrderCreated(OrderCreatedEvent event) {
        rabbitTemplate.convertAndSend(topology.exchanges().ordersDirect(), topology.routingKeys().orderCreated(), event,
                message -> {
                    message.getMessageProperties().setHeader("__TypeId__", "orderCreatedEvent");
                    return message;
                });
        log.info("Publicado {}: orderId={}", topology.routingKeys().orderCreated(), event.orderId());
    }

    @Override
    public void publishOrderStatusChanged(OrderStatusChangedEvent event) {
        String routingKey = topology.statusRoutingKey(event.newStatus());
        rabbitTemplate.convertAndSend(topology.exchanges().ordersStatusTopic(), routingKey, event);
        log.info("Publicado {}: orderId={} {} -> {}",
                routingKey, event.orderId(), event.previousStatus(), event.newStatus());
    }
}
