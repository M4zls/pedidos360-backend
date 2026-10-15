package com.pedidos360.orders.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.pedidos360.orders.messaging.RabbitTopology.*;

/**
 * Declara la topologia que orders-service necesita: los dos exchanges del
 * dominio (los produce) y las dos colas que el propio servicio consume
 * (notificaciones al cliente, aviso a despacho), cada una con su DLX/DLQ.
 * RabbitAdmin declara estos beans contra el broker apenas arranca el contexto.
 */
@Configuration
public class MessagingTopologyConfig {

    @Bean
    public DirectExchange ordersDirectExchange() {
        return new DirectExchange(ORDERS_DIRECT_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange ordersStatusTopicExchange() {
        return new TopicExchange(ORDERS_STATUS_TOPIC_EXCHANGE, true, false);
    }

    // ---- orders.notifications (avisa al cliente) ----

    @Bean
    public DirectExchange ordersNotificationsDlx() {
        return new DirectExchange(ORDERS_NOTIFICATIONS_DLX, true, false);
    }

    @Bean
    public Queue ordersNotificationsDlq() {
        return QueueBuilder.durable(ORDERS_NOTIFICATIONS_DLQ).build();
    }

    @Bean
    public Binding ordersNotificationsDlqBinding() {
        return BindingBuilder.bind(ordersNotificationsDlq())
                .to(ordersNotificationsDlx())
                .with(ORDERS_NOTIFICATIONS_DLQ);
    }

    @Bean
    public Queue ordersNotificationsQueue() {
        return QueueBuilder.durable(ORDERS_NOTIFICATIONS_QUEUE)
                .withArgument("x-dead-letter-exchange", ORDERS_NOTIFICATIONS_DLX)
                .withArgument("x-dead-letter-routing-key", ORDERS_NOTIFICATIONS_DLQ)
                .build();
    }

    /** Se le notifica al cliente en estos hitos (coincide con OrderService.NOTIFIABLE_LABELS). */
    @Bean
    public Binding ordersNotificationsListoBinding() {
        return BindingBuilder.bind(ordersNotificationsQueue())
                .to(ordersStatusTopicExchange())
                .with(statusRoutingKey("LISTO"));
    }

    @Bean
    public Binding ordersNotificationsDespachadoBinding() {
        return BindingBuilder.bind(ordersNotificationsQueue())
                .to(ordersStatusTopicExchange())
                .with(statusRoutingKey("DESPACHADO"));
    }

    @Bean
    public Binding ordersNotificationsEntregadoBinding() {
        return BindingBuilder.bind(ordersNotificationsQueue())
                .to(ordersStatusTopicExchange())
                .with(statusRoutingKey("ENTREGADO"));
    }

    @Bean
    public Binding ordersNotificationsCanceladoBinding() {
        return BindingBuilder.bind(ordersNotificationsQueue())
                .to(ordersStatusTopicExchange())
                .with(statusRoutingKey("CANCELADO"));
    }

    // ---- dispatch.ready (avisa a despacho) ----

    @Bean
    public DirectExchange dispatchReadyDlx() {
        return new DirectExchange(DISPATCH_READY_DLX, true, false);
    }

    @Bean
    public Queue dispatchReadyDlq() {
        return QueueBuilder.durable(DISPATCH_READY_DLQ).build();
    }

    @Bean
    public Binding dispatchReadyDlqBinding() {
        return BindingBuilder.bind(dispatchReadyDlq())
                .to(dispatchReadyDlx())
                .with(DISPATCH_READY_DLQ);
    }

    @Bean
    public Queue dispatchReadyQueue() {
        return QueueBuilder.durable(DISPATCH_READY_QUEUE)
                .withArgument("x-dead-letter-exchange", DISPATCH_READY_DLX)
                .withArgument("x-dead-letter-routing-key", DISPATCH_READY_DLQ)
                .build();
    }

    @Bean
    public Binding dispatchReadyBinding() {
        return BindingBuilder.bind(dispatchReadyQueue())
                .to(ordersStatusTopicExchange())
                .with(statusRoutingKey("LISTO"));
    }
}
