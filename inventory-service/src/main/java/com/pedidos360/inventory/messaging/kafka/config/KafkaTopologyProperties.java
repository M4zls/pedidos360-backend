package com.pedidos360.inventory.messaging.kafka.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Nombres de topicos de Kafka y configuracion del cliente, leidos de
 * {@code app.kafka} en application.yml. Configuracion centralizada: todo el
 * codigo de mensajeria Kafka sale de aca, nunca de un literal suelto (mismo
 * criterio que {@code RabbitTopologyProperties} para RabbitMQ).
 */
@Validated
@ConfigurationProperties(prefix = "app.kafka")
public record KafkaTopologyProperties(
        @NotBlank String bootstrapServers,
        @NotNull @Valid Topics topics,
        @NotNull @Valid Consumer consumer) {

    public record Topics(
            @NotNull @Valid OwnedTopic inventoryEvents,
            @NotNull @Valid ConsumedTopic ordersEvents) {
    }

    /** Un topico que este servicio PRODUCE y por lo tanto administra (particiones, replicas, retencion). */
    public record OwnedTopic(
            @NotBlank String name,
            @Positive int partitions,
            @Positive short replicationFactor,
            @Positive long retentionMs) {
    }

    /** Un topico que este servicio solo CONSUME; lo crea y administra quien lo produce. */
    public record ConsumedTopic(@NotBlank String name) {
    }

    public record Consumer(@NotBlank String groupId) {
    }
}
