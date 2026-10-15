package com.pedidos360.identity.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.pedidos360.identity.messaging.RabbitTopology.*;

/**
 * Declara la topologia de usuarios: el exchange {@code users.topic} (lo
 * produce auth-service) y las dos colas que el propio servicio consume
 * (auditoria y alertas de ADMIN), cada una con su DLX/DLQ.
 * RabbitAdmin declara estos beans contra el broker apenas arranca el contexto.
 */
@Configuration
public class MessagingTopologyConfig {

    @Bean
    public TopicExchange usersTopicExchange() {
        return new TopicExchange(USERS_TOPIC_EXCHANGE, true, false);
    }

    // ---- users.audit (toda la actividad de usuarios) ----

    @Bean
    public DirectExchange usersAuditDlx() {
        return new DirectExchange(USERS_AUDIT_DLX, true, false);
    }

    @Bean
    public Queue usersAuditDlq() {
        return QueueBuilder.durable(USERS_AUDIT_DLQ).build();
    }

    @Bean
    public Binding usersAuditDlqBinding() {
        return BindingBuilder.bind(usersAuditDlq())
                .to(usersAuditDlx())
                .with(USERS_AUDIT_DLQ);
    }

    @Bean
    public Queue usersAuditQueue() {
        return QueueBuilder.durable(USERS_AUDIT_QUEUE)
                .withArgument("x-dead-letter-exchange", USERS_AUDIT_DLX)
                .withArgument("x-dead-letter-routing-key", USERS_AUDIT_DLQ)
                .build();
    }

    @Bean
    public Binding usersAuditBinding() {
        return BindingBuilder.bind(usersAuditQueue())
                .to(usersTopicExchange())
                .with(USERS_AUDIT_BINDING);
    }

    // ---- users.admin-alerts (aparece o asciende un ADMIN) ----

    @Bean
    public DirectExchange usersAdminAlertsDlx() {
        return new DirectExchange(USERS_ADMIN_ALERTS_DLX, true, false);
    }

    @Bean
    public Queue usersAdminAlertsDlq() {
        return QueueBuilder.durable(USERS_ADMIN_ALERTS_DLQ).build();
    }

    @Bean
    public Binding usersAdminAlertsDlqBinding() {
        return BindingBuilder.bind(usersAdminAlertsDlq())
                .to(usersAdminAlertsDlx())
                .with(USERS_ADMIN_ALERTS_DLQ);
    }

    @Bean
    public Queue usersAdminAlertsQueue() {
        return QueueBuilder.durable(USERS_ADMIN_ALERTS_QUEUE)
                .withArgument("x-dead-letter-exchange", USERS_ADMIN_ALERTS_DLX)
                .withArgument("x-dead-letter-routing-key", USERS_ADMIN_ALERTS_DLQ)
                .build();
    }

    @Bean
    public Binding usersAdminAlertsBinding() {
        return BindingBuilder.bind(usersAdminAlertsQueue())
                .to(usersTopicExchange())
                .with(USERS_ADMIN_ALERTS_BINDING);
    }
}
