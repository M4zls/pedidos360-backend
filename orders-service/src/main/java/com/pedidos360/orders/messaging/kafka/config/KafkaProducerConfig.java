package com.pedidos360.orders.messaging.kafka.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Cliente productor de Kafka: conexion a los brokers, serializacion JSON y el
 * topico que este servicio produce ({@code orders.events}). El bean
 * {@code NewTopic} de abajo hace que {@link KafkaAdmin} lo cree al arrancar
 * si todavia no existe, con sus particiones, factor de replicacion y
 * retencion ya definidos aca.
 */
@Configuration
@EnableConfigurationProperties(KafkaTopologyProperties.class)
public class KafkaProducerConfig {

    @Bean
    public KafkaAdmin kafkaAdmin(KafkaTopologyProperties topology) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, topology.bootstrapServers());
        // No tira la app abajo si el cluster de Kafka todavia no esta levantado
        // (docker-compose.kafka.yml es un stack aparte del de este servicio).
        KafkaAdmin admin = new KafkaAdmin(configs);
        admin.setFatalIfBrokerNotAvailable(false);
        admin.setOperationTimeout(5);
        return admin;
    }

    @Bean
    public org.apache.kafka.clients.admin.NewTopic ordersEventsTopic(KafkaTopologyProperties topology) {
        KafkaTopologyProperties.OwnedTopic t = topology.topics().ordersEvents();
        return TopicBuilder.name(t.name())
                .partitions(t.partitions())
                .replicas(t.replicationFactor())
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(t.retentionMs()))
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .build();
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory(KafkaTopologyProperties topology) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, topology.bootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // "all": espera confirmacion de todas las replicas in-sync antes de dar
        // por publicado el mensaje (coherente con el factor de replicacion 2).
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        return new DefaultKafkaProducerFactory<>(config, new StringSerializer(), new JsonSerializer<>(objectMapper));
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
