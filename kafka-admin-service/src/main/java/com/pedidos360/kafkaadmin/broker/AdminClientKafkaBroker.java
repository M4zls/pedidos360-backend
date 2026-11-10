package com.pedidos360.kafkaadmin.broker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.pedidos360.kafkaadmin.config.KafkaAdminProperties;

/** Implementacion del broker contra el cluster de Kafka via AdminClient. */
@Component
public class AdminClientKafkaBroker implements KafkaAdminBroker {

    private static final Logger log = LoggerFactory.getLogger(AdminClientKafkaBroker.class);

    private final Admin admin;
    private final long timeoutMs;

    public AdminClientKafkaBroker(Admin admin, KafkaAdminProperties properties) {
        this.admin = admin;
        this.timeoutMs = properties.requestTimeoutMs();
    }

    // ---- topicos ----

    @Override
    public List<TopicInfo> listTopics() {
        var names = execute(() -> admin.listTopics(new ListTopicsOptions().listInternal(false))
                .names());
        return names.stream().map(this::getTopic).toList();
    }

    @Override
    public TopicInfo getTopic(String name) {
        Map<String, TopicDescription> described = execute(() -> admin.describeTopics(List.of(name)).allTopicNames());
        TopicDescription description = described.get(name);
        if (description == null) {
            throw new KafkaAdminException(KafkaAdminException.Kind.NOT_FOUND, "El topico '" + name + "' no existe");
        }

        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
        Map<ConfigResource, Config> configs = execute(() -> admin.describeConfigs(List.of(resource)).all());
        Map<String, String> configMap = new HashMap<>();
        for (ConfigEntry entry : configs.get(resource).entries()) {
            // Solo lo que alguien configuro explicitamente; los defaults del broker
            // (DEFAULT_CONFIG) ensucian la respuesta sin aportar nada util aca.
            if (entry.source() == ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG) {
                configMap.put(entry.name(), entry.value());
            }
        }

        int partitions = description.partitions().size();
        short replicationFactor = (short) description.partitions().get(0).replicas().size();
        return new TopicInfo(name, partitions, replicationFactor, configMap);
    }

    @Override
    public void createTopic(String name, int partitions, short replicationFactor, Map<String, String> config) {
        NewTopic newTopic = new NewTopic(name, partitions, replicationFactor);
        if (config != null && !config.isEmpty()) {
            newTopic.configs(config);
        }
        execute(() -> admin.createTopics(List.of(newTopic)).all());
        log.info("Topico creado: name={} partitions={} replicationFactor={} config={}",
                name, partitions, replicationFactor, config);
    }

    @Override
    public void updateTopicConfig(String name, Map<String, String> config) {
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
        Collection<AlterConfigOp> ops = new ArrayList<>();
        config.forEach((key, value) -> ops.add(new AlterConfigOp(new ConfigEntry(key, value), AlterConfigOp.OpType.SET)));
        execute(() -> admin.incrementalAlterConfigs(Map.of(resource, ops)).all());
        log.info("Configuracion de topico actualizada: name={} config={}", name, config);
    }

    @Override
    public void deleteTopic(String name) {
        execute(() -> admin.deleteTopics(List.of(name)).all());
        log.info("Topico eliminado: name={}", name);
    }

    // ---- consumer groups ----

    @Override
    public List<ConsumerGroupInfo> listConsumerGroups() {
        Collection<ConsumerGroupListing> listings = execute(() -> admin.listConsumerGroups().all());
        return listings.stream().map(l -> getConsumerGroup(l.groupId())).toList();
    }

    @Override
    public ConsumerGroupInfo getConsumerGroup(String groupId) {
        Map<String, ConsumerGroupDescription> described =
                execute(() -> admin.describeConsumerGroups(List.of(groupId)).all());
        ConsumerGroupDescription description = described.get(groupId);
        if (description == null) {
            throw new KafkaAdminException(KafkaAdminException.Kind.NOT_FOUND, "El consumer group '" + groupId + "' no existe");
        }
        List<String> members = description.members().stream()
                .map(MemberDescription::consumerId)
                .toList();
        // El lag por particion requiere cruzar listConsumerGroupOffsets con los
        // end offsets de cada topico; queda fuera de este alcance (ver ConsumerGroupInfo).
        return new ConsumerGroupInfo(groupId, description.state().toString(), members, null);
    }

    private <T> T execute(Supplier<KafkaFuture<T>> call) {
        try {
            return call.get().get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (ExecutionException ex) {
            throw translate(ex.getCause() != null ? ex.getCause() : ex);
        } catch (java.util.concurrent.TimeoutException ex) {
            throw new KafkaAdminException(KafkaAdminException.Kind.UPSTREAM, "Timeout hablando con el cluster de Kafka", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new KafkaAdminException(KafkaAdminException.Kind.UPSTREAM, "Interrumpido hablando con Kafka", ex);
        }
    }

    private static KafkaAdminException translate(Throwable cause) {
        return switch (cause) {
            case TopicExistsException ex -> new KafkaAdminException(KafkaAdminException.Kind.CONFLICT, ex.getMessage(), ex);
            case UnknownTopicOrPartitionException ex ->
                    new KafkaAdminException(KafkaAdminException.Kind.NOT_FOUND, ex.getMessage(), ex);
            case GroupIdNotFoundException ex ->
                    new KafkaAdminException(KafkaAdminException.Kind.NOT_FOUND, ex.getMessage(), ex);
            case InvalidReplicationFactorException ex ->
                    new KafkaAdminException(KafkaAdminException.Kind.BAD_REQUEST, ex.getMessage(), ex);
            case InvalidPartitionsException ex ->
                    new KafkaAdminException(KafkaAdminException.Kind.BAD_REQUEST, ex.getMessage(), ex);
            // InvalidTopicException extends InvalidConfigurationException: un solo
            // case cubre ambas (nombre de topico invalido es, en esencia, config invalida).
            case InvalidConfigurationException ex ->
                    new KafkaAdminException(KafkaAdminException.Kind.BAD_REQUEST, ex.getMessage(), ex);
            case TimeoutException ex ->
                    new KafkaAdminException(KafkaAdminException.Kind.UPSTREAM, "Timeout del broker: " + ex.getMessage(), ex);
            default -> new KafkaAdminException(KafkaAdminException.Kind.UPSTREAM,
                    "Error hablando con el cluster de Kafka: " + cause.getMessage(), cause);
        };
    }
}
