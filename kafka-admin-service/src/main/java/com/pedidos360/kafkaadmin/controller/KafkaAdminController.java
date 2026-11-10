package com.pedidos360.kafkaadmin.controller;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.pedidos360.kafkaadmin.broker.ConsumerGroupInfo;
import com.pedidos360.kafkaadmin.broker.TopicInfo;
import com.pedidos360.kafkaadmin.controller.dto.CreateTopicRequest;
import com.pedidos360.kafkaadmin.controller.dto.UpdateTopicConfigRequest;
import com.pedidos360.kafkaadmin.controller.validation.KafkaTopicName;
import com.pedidos360.kafkaadmin.service.KafkaAdminService;

/**
 * Administracion de Kafka: topicos, sus configuraciones y consumer groups.
 * Solo traduce HTTP y valida la entrada; la logica esta en
 * {@link KafkaAdminService}. Solo ADMIN puede tocar esto (ver SecurityConfig).
 *
 * <pre>
 * GET    /topics                 GET    /consumer-groups
 * GET    /topics/{name}           GET    /consumer-groups/{groupId}
 * POST   /topics
 * PATCH  /topics/{name}/config
 * DELETE /topics/{name}
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/kafka")
public class KafkaAdminController {

    private final KafkaAdminService kafkaAdmin;

    public KafkaAdminController(KafkaAdminService kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    // ---- topicos ----

    @GetMapping("/topics")
    public List<TopicInfo> listTopics() {
        return kafkaAdmin.listTopics();
    }

    @GetMapping("/topics/{name}")
    public TopicInfo getTopic(@PathVariable @KafkaTopicName String name) {
        return kafkaAdmin.getTopic(name);
    }

    @PostMapping("/topics")
    public ResponseEntity<Void> createTopic(@Valid @RequestBody CreateTopicRequest request) {
        kafkaAdmin.createTopic(request.name(), request.partitions(), request.replicationFactor(), request.config());
        return ResponseEntity.created(locationOf(request.name())).build();
    }

    @PatchMapping("/topics/{name}/config")
    public ResponseEntity<Void> updateTopicConfig(
            @PathVariable @KafkaTopicName String name,
            @Valid @RequestBody UpdateTopicConfigRequest request) {
        kafkaAdmin.updateTopicConfig(name, request.config());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/topics/{name}")
    public ResponseEntity<Void> deleteTopic(@PathVariable @KafkaTopicName String name) {
        kafkaAdmin.deleteTopic(name);
        return ResponseEntity.noContent().build();
    }

    // ---- consumer groups ----

    @GetMapping("/consumer-groups")
    public List<ConsumerGroupInfo> listConsumerGroups() {
        return kafkaAdmin.listConsumerGroups();
    }

    @GetMapping("/consumer-groups/{groupId}")
    public ConsumerGroupInfo getConsumerGroup(@PathVariable String groupId) {
        return kafkaAdmin.getConsumerGroup(groupId);
    }

    private static URI locationOf(String name) {
        return ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{name}")
                .buildAndExpand(name)
                .toUri();
    }
}
