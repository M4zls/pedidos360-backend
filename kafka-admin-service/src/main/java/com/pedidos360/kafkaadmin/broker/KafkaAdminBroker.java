package com.pedidos360.kafkaadmin.broker;

import java.util.List;
import java.util.Map;

/** Puerto hacia el cluster de Kafka. El servicio de administracion depende de esta abstraccion. */
public interface KafkaAdminBroker {

    List<TopicInfo> listTopics();

    TopicInfo getTopic(String name);

    void createTopic(String name, int partitions, short replicationFactor, Map<String, String> config);

    void updateTopicConfig(String name, Map<String, String> config);

    void deleteTopic(String name);

    List<ConsumerGroupInfo> listConsumerGroups();

    ConsumerGroupInfo getConsumerGroup(String groupId);
}
