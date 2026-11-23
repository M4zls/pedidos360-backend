package com.pedidos360.analytics.messaging.kafka.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Nombres de topicos y ventanas de la topologia, leidos de {@code app.kafka}
 * en application.yml. Mismo criterio que {@code KafkaTopologyProperties} en
 * orders/inventory-service: configuracion centralizada, nunca un literal
 * suelto en el codigo.
 */
@Validated
@ConfigurationProperties(prefix = "app.kafka")
public record AnalyticsKafkaProperties(
        @NotBlank String bootstrapServers,
        @NotNull @Valid Topics topics,
        @Positive long joinWindowMs,
        @Positive long ordersPerMinuteWindowMs) {

    public record Topics(
            @NotNull @Valid ConsumedTopic ordersEvents,
            @NotNull @Valid ConsumedTopic inventoryEvents,
            @NotNull @Valid OwnedTopic orderFulfillmentEvents) {
    }

    /** Un topico que este servicio solo CONSUME; lo crea y administra quien lo produce. */
    public record ConsumedTopic(@NotBlank String name) {
    }

    /** Un topico que este servicio PRODUCE y por lo tanto administra (particiones, replicas, retencion). */
    public record OwnedTopic(
            @NotBlank String name,
            @Positive int partitions,
            @Positive short replicationFactor,
            @Positive long retentionMs) {
    }
}
