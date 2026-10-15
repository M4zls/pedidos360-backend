package com.pedidos360.rabbitmqadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuracion centralizada de acceso a la Management HTTP API de RabbitMQ. */
@ConfigurationProperties(prefix = "app.rabbitmq.management")
public record RabbitMqManagementProperties(
        String host,
        int port,
        String username,
        String password,
        String vhost) {

    public String baseUrl() {
        return "http://" + host + ":" + port + "/api";
    }
}
