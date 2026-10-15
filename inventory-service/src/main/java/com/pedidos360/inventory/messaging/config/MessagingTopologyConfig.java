package com.pedidos360.inventory.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.pedidos360.inventory.messaging.RabbitTopology.*;

/**
 * Declara el exchange compartido con orders-service (productor) y la cola
 * propia de inventory-service, con su DLX/DLQ.
 */
@Configuration
public class MessagingTopologyConfig {

    @Bean
    public DirectExchange ordersDirectExchange() {
        return new DirectExchange(ORDERS_DIRECT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange inventoryStockReserveDlx() {
        return new DirectExchange(INVENTORY_STOCK_RESERVE_DLX, true, false);
    }

    @Bean
    public Queue inventoryStockReserveDlq() {
        return QueueBuilder.durable(INVENTORY_STOCK_RESERVE_DLQ).build();
    }

    @Bean
    public Binding inventoryStockReserveDlqBinding() {
        return BindingBuilder.bind(inventoryStockReserveDlq())
                .to(inventoryStockReserveDlx())
                .with(INVENTORY_STOCK_RESERVE_DLQ);
    }

    @Bean
    public Queue inventoryStockReserveQueue() {
        return QueueBuilder.durable(INVENTORY_STOCK_RESERVE_QUEUE)
                .withArgument("x-dead-letter-exchange", INVENTORY_STOCK_RESERVE_DLX)
                .withArgument("x-dead-letter-routing-key", INVENTORY_STOCK_RESERVE_DLQ)
                .build();
    }

    @Bean
    public Binding inventoryStockReserveBinding() {
        return BindingBuilder.bind(inventoryStockReserveQueue())
                .to(ordersDirectExchange())
                .with(ORDER_CREATED_ROUTING_KEY);
    }
}
