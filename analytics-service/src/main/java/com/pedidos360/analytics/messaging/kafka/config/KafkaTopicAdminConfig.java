package com.pedidos360.analytics.messaging.kafka.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.config.TopicConfig;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Crea {@code order-fulfillment.events} al arrancar si todavia no existe (con
 * sus particiones, factor de replicacion y retencion ya definidos aca), igual
 * que {@code KafkaProducerConfig} en orders/inventory-service.
 * {@code orders.events} e {@code inventory.events} no se tocan: ya los crean
 * y administran quienes los producen.
 */
@Configuration
@EnableConfigurationProperties(AnalyticsKafkaProperties.class)
public class KafkaTopicAdminConfig {

    @Bean
    public KafkaAdmin kafkaAdmin(AnalyticsKafkaProperties properties) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, properties.bootstrapServers());
        // No tira la app abajo si el cluster de Kafka todavia no esta levantado
        // (docker-compose.kafka.yml es un stack aparte del de este servicio).
        KafkaAdmin admin = new KafkaAdmin(configs);
        admin.setFatalIfBrokerNotAvailable(false);
        admin.setOperationTimeout(5);
        return admin;
    }

    @Bean
    public org.apache.kafka.clients.admin.NewTopic orderFulfillmentEventsTopic(AnalyticsKafkaProperties properties) {
        AnalyticsKafkaProperties.OwnedTopic t = properties.topics().orderFulfillmentEvents();
        return TopicBuilder.name(t.name())
                .partitions(t.partitions())
                .replicas(t.replicationFactor())
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(t.retentionMs()))
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .build();
    }
}
