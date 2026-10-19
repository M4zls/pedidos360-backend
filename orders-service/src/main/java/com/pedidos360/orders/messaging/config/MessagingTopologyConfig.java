package com.pedidos360.orders.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.pedidos360.orders.messaging.config.RabbitTopologyProperties.QueueTopology;

/**
 * Declara la topologia que orders-service necesita: los dos exchanges del
 * dominio (los produce) y las dos colas que el propio servicio consume
 * (notificaciones al cliente, aviso a despacho), cada una con su DLX/DLQ.
 * Los nombres salen de application.yml ({@link RabbitTopologyProperties}).
 * RabbitAdmin declara estos beans contra el broker apenas arranca el contexto.
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
    public TopicExchange ordersStatusTopicExchange() {
        return new TopicExchange(topology.exchanges().ordersStatusTopic(), true, false);
    }

    // ---- orders.notifications (avisa al cliente) ----

    @Bean
    public DirectExchange ordersNotificationsDlx() {
        return new DirectExchange(notifications().dlx(), true, false);
    }

    @Bean
    public Queue ordersNotificationsDlq() {
        return QueueBuilder.durable(notifications().dlq()).build();
    }

    @Bean
    public Binding ordersNotificationsDlqBinding() {
        return BindingBuilder.bind(ordersNotificationsDlq())
                .to(ordersNotificationsDlx())
                .with(notifications().dlq());
    }

    @Bean
    public Queue ordersNotificationsQueue() {
        return QueueBuilder.durable(notifications().name())
                .withArgument("x-dead-letter-exchange", notifications().dlx())
                .withArgument("x-dead-letter-routing-key", notifications().dlq())
                .build();
    }

    /** Se le notifica al cliente en estos hitos (coincide con OrderService.NOTIFIABLE_LABELS). */
    @Bean
    public Binding ordersNotificationsListoBinding() {
        return BindingBuilder.bind(ordersNotificationsQueue())
                .to(ordersStatusTopicExchange())
                .with(topology.statusRoutingKey("LISTO"));
    }

    @Bean
    public Binding ordersNotificationsDespachadoBinding() {
        return BindingBuilder.bind(ordersNotificationsQueue())
                .to(ordersStatusTopicExchange())
                .with(topology.statusRoutingKey("DESPACHADO"));
    }

    @Bean
    public Binding ordersNotificationsEntregadoBinding() {
        return BindingBuilder.bind(ordersNotificationsQueue())
                .to(ordersStatusTopicExchange())
                .with(topology.statusRoutingKey("ENTREGADO"));
    }

    @Bean
    public Binding ordersNotificationsCanceladoBinding() {
        return BindingBuilder.bind(ordersNotificationsQueue())
                .to(ordersStatusTopicExchange())
                .with(topology.statusRoutingKey("CANCELADO"));
    }

    // ---- dispatch.ready (avisa a despacho) ----

    @Bean
    public DirectExchange dispatchReadyDlx() {
        return new DirectExchange(dispatchReady().dlx(), true, false);
    }

    @Bean
    public Queue dispatchReadyDlq() {
        return QueueBuilder.durable(dispatchReady().dlq()).build();
    }

    @Bean
    public Binding dispatchReadyDlqBinding() {
        return BindingBuilder.bind(dispatchReadyDlq())
                .to(dispatchReadyDlx())
                .with(dispatchReady().dlq());
    }

    @Bean
    public Queue dispatchReadyQueue() {
        return QueueBuilder.durable(dispatchReady().name())
                .withArgument("x-dead-letter-exchange", dispatchReady().dlx())
                .withArgument("x-dead-letter-routing-key", dispatchReady().dlq())
                .build();
    }

    @Bean
    public Binding dispatchReadyBinding() {
        return BindingBuilder.bind(dispatchReadyQueue())
                .to(ordersStatusTopicExchange())
                .with(topology.statusRoutingKey("LISTO"));
    }

    private QueueTopology notifications() {
        return topology.queues().ordersNotifications();
    }

    private QueueTopology dispatchReady() {
        return topology.queues().dispatchReady();
    }
}
