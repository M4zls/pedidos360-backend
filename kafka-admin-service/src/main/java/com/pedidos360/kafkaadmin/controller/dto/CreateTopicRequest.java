package com.pedidos360.kafkaadmin.controller.dto;

import java.util.Map;

import jakarta.validation.constraints.Min;

import com.pedidos360.kafkaadmin.controller.validation.KafkaTopicName;

/**
 * partitions y replicationFactor son opcionales: por defecto 1 particion,
 * factor de replicacion 1. config admite cualquier parametro de topico de
 * Kafka (ej. retention.ms, cleanup.policy, min.insync.replicas).
 */
public record CreateTopicRequest(
        @KafkaTopicName String name,
        @Min(1) Integer partitions,
        @Min(1) Short replicationFactor,
        Map<String, String> config) {

    public CreateTopicRequest {
        partitions = partitions == null ? 1 : partitions;
        replicationFactor = replicationFactor == null ? (short) 1 : replicationFactor;
        config = config == null ? Map.of() : config;
    }
}
