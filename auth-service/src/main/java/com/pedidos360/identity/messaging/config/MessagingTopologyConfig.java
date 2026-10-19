package com.pedidos360.identity.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.pedidos360.identity.messaging.config.RabbitTopologyProperties.QueueTopology;

/**
 * Declara la topologia de usuarios: el exchange {@code users.topic} (lo
 * produce auth-service) y las dos colas que el propio servicio consume
 * (auditoria y alertas de ADMIN), cada una con su DLX/DLQ. Los nombres salen
 * de application.yml ({@link RabbitTopologyProperties}).
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
    public TopicExchange usersTopicExchange() {
        return new TopicExchange(topology.exchanges().usersTopic(), true, false);
    }

    // ---- users.audit (toda la actividad de usuarios) ----

    @Bean
    public DirectExchange usersAuditDlx() {
        return new DirectExchange(audit().dlx(), true, false);
    }

    @Bean
    public Queue usersAuditDlq() {
        return QueueBuilder.durable(audit().dlq()).build();
    }

    @Bean
    public Binding usersAuditDlqBinding() {
        return BindingBuilder.bind(usersAuditDlq())
                .to(usersAuditDlx())
                .with(audit().dlq());
    }

    @Bean
    public Queue usersAuditQueue() {
        return QueueBuilder.durable(audit().name())
                .withArgument("x-dead-letter-exchange", audit().dlx())
                .withArgument("x-dead-letter-routing-key", audit().dlq())
                .build();
    }

    @Bean
    public Binding usersAuditBinding() {
        return BindingBuilder.bind(usersAuditQueue())
                .to(usersTopicExchange())
                .with(audit().bindingKey());
    }

    // ---- users.admin-alerts (aparece o asciende un ADMIN) ----

    @Bean
    public DirectExchange usersAdminAlertsDlx() {
        return new DirectExchange(adminAlerts().dlx(), true, false);
    }

    @Bean
    public Queue usersAdminAlertsDlq() {
        return QueueBuilder.durable(adminAlerts().dlq()).build();
    }

    @Bean
    public Binding usersAdminAlertsDlqBinding() {
        return BindingBuilder.bind(usersAdminAlertsDlq())
                .to(usersAdminAlertsDlx())
                .with(adminAlerts().dlq());
    }

    @Bean
    public Queue usersAdminAlertsQueue() {
        return QueueBuilder.durable(adminAlerts().name())
                .withArgument("x-dead-letter-exchange", adminAlerts().dlx())
                .withArgument("x-dead-letter-routing-key", adminAlerts().dlq())
                .build();
    }

    @Bean
    public Binding usersAdminAlertsBinding() {
        return BindingBuilder.bind(usersAdminAlertsQueue())
                .to(usersTopicExchange())
                .with(adminAlerts().bindingKey());
    }

    private QueueTopology audit() {
        return topology.queues().audit();
    }

    private QueueTopology adminAlerts() {
        return topology.queues().adminAlerts();
    }
}
