package com.pedidos360.inventory.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.pedidos360.inventory.messaging.config.RabbitTopologyProperties.QueueTopology;

/**
 * Declara el exchange compartido con orders-service (productor) y la cola
 * propia de inventory-service, con su DLX/DLQ. Los nombres salen de
 * application.yml ({@link RabbitTopologyProperties}).
 */
@Configuration
@EnableConfigurationProperties(RabbitTopologyProperties.class)
public class MessagingTopologyConfig {

    private final RabbitTopologyProperties topology;

    public MessagingTopologyConfig(RabbitTopologyProperties topology) {
        this.topology = topology;
    }

    @Bean
    public DirectExchange ordersDirectExchange() {
        return new DirectExchange(topology.exchanges().ordersDirect(), true, false);
    }

    @Bean
    public DirectExchange inventoryStockReserveDlx() {
        return new DirectExchange(stockReserve().dlx(), true, false);
    }

    @Bean
    public Queue inventoryStockReserveDlq() {
        return QueueBuilder.durable(stockReserve().dlq()).build();
    }

    @Bean
    public Binding inventoryStockReserveDlqBinding() {
        return BindingBuilder.bind(inventoryStockReserveDlq())
                .to(inventoryStockReserveDlx())
                .with(stockReserve().dlq());
    }

    @Bean
    public Queue inventoryStockReserveQueue() {
        return QueueBuilder.durable(stockReserve().name())
                .withArgument("x-dead-letter-exchange", stockReserve().dlx())
                .withArgument("x-dead-letter-routing-key", stockReserve().dlq())
                .build();
    }

    @Bean
    public Binding inventoryStockReserveBinding() {
        return BindingBuilder.bind(inventoryStockReserveQueue())
                .to(ordersDirectExchange())
                .with(topology.routingKeys().orderCreated());
    }

    private QueueTopology stockReserve() {
        return topology.queues().stockReserve();
    }
}
