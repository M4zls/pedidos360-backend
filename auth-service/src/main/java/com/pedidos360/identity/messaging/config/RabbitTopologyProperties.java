package com.pedidos360.identity.messaging.config;

import java.util.Locale;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Nombres de exchanges, colas y routing keys del dominio de usuarios, leidos
 * de {@code app.rabbitmq} en application.yml. Configuracion centralizada: todo
 * el codigo de mensajeria sale de aca, nunca de un literal suelto. Si falta
 * algun nombre, el servicio no arranca.
 */
@Validated
@ConfigurationProperties(prefix = "app.rabbitmq")
public record RabbitTopologyProperties(
        @NotNull @Valid Exchanges exchanges,
        @NotNull @Valid RoutingKeys routingKeys,
        @NotNull @Valid Queues queues) {

    /**
     * @param usersTopic topic: eventos de usuarios. Routing key
     *                   {@code user.<evento>.<rol>}, asi un consumidor puede
     *                   filtrar por evento, por rol, o por ambos.
     */
    public record Exchanges(@NotBlank String usersTopic) {
    }

    /** @param userPrefix prefijo de {@code <prefijo><evento>.<rol>} */
    public record RoutingKeys(@NotBlank String userPrefix) {
    }

    /**
     * @param audit       auditoria: recibe todos los eventos de usuarios
     * @param adminAlerts alertas: solo eventos cuyo rol resultante es ADMIN
     */
    public record Queues(@NotNull @Valid QueueTopology audit, @NotNull @Valid QueueTopology adminAlerts) {
    }

    /**
     * Una cola con su dead-letter-exchange, su dead-letter-queue y el patron
     * con el que se bindea a {@code users.topic}.
     */
    public record QueueTopology(@NotBlank String name, @NotBlank String dlx, @NotBlank String dlq,
            @NotBlank String bindingKey) {
    }

    public String userRoutingKey(String eventType, String role) {
        return routingKeys.userPrefix() + eventType + "." + role.toLowerCase(Locale.ROOT);
    }
}
