package com.pedidos360.kafkaadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuracion centralizada de acceso al cluster de Kafka (AdminClient). */
@ConfigurationProperties(prefix = "app.kafka.admin")
public record KafkaAdminProperties(String bootstrapServers, int requestTimeoutMs) {
}
