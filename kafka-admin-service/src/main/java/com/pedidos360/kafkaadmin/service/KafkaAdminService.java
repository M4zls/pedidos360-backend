package com.pedidos360.kafkaadmin.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.pedidos360.kafkaadmin.broker.ConsumerGroupInfo;
import com.pedidos360.kafkaadmin.broker.KafkaAdminBroker;
import com.pedidos360.kafkaadmin.broker.TopicInfo;

/**
 * Logica de administracion de Kafka: crear, consultar, reconfigurar y borrar
 * topicos, e inspeccionar consumer groups. El controller solo traduce HTTP;
 * las reglas (no pisar un topico existente) viven aca. Habla con el cluster a
 * traves de {@link KafkaAdminBroker}.
 */
@Service
public class KafkaAdminService {

    private final KafkaAdminBroker broker;

    public KafkaAdminService(KafkaAdminBroker broker) {
        this.broker = broker;
    }

    // ---- topicos ----

    public List<TopicInfo> listTopics() {
        return broker.listTopics();
    }

    public TopicInfo getTopic(String name) {
        return broker.getTopic(name);
    }

    public void createTopic(String name, int partitions, short replicationFactor, Map<String, String> config) {
        if (topicExists(name)) {
            throw new TopologyConflictException("El topico '%s' ya existe".formatted(name));
        }
        broker.createTopic(name, partitions, replicationFactor, config);
    }

    public void updateTopicConfig(String name, Map<String, String> config) {
        broker.updateTopicConfig(name, config);
    }

    public void deleteTopic(String name) {
        broker.deleteTopic(name);
    }

    // ---- consumer groups ----

    public List<ConsumerGroupInfo> listConsumerGroups() {
        return broker.listConsumerGroups();
    }

    public ConsumerGroupInfo getConsumerGroup(String groupId) {
        return broker.getConsumerGroup(groupId);
    }

    private boolean topicExists(String name) {
        return broker.listTopics().stream().anyMatch(t -> t.name().equals(name));
    }
}
