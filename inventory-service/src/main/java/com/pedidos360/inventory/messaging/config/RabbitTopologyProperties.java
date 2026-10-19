package com.pedidos360.inventory.messaging.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Nombres de exchanges, colas y routing keys que le importan a
 * inventory-service, leidos de {@code app.rabbitmq} en application.yml.
 * Configuracion centralizada: todo el codigo de mensajeria sale de aca, nunca
 * de un literal suelto. Si falta algun nombre, el servicio no arranca.
 */
@Validated
@ConfigurationProperties(prefix = "app.rabbitmq")
public record RabbitTopologyProperties(
        @NotNull @Valid Exchanges exchanges,
        @NotNull @Valid RoutingKeys routingKeys,
        @NotNull @Valid Queues queues) {

    /**
     * @param ordersDirect exchange de orders-service (su productor). Declararlo
     *                     aca de nuevo es idempotente mientras coincidan sus
     *                     propiedades (nombre, tipo, durable).
     */
    public record Exchanges(@NotBlank String ordersDirect) {
    }

    /** @param orderCreated la publica orders-service al crear un pedido */
    public record RoutingKeys(@NotBlank String orderCreated) {
    }

    /** @param stockReserve cola propia: cada pedido nuevo descuenta su stock */
    public record Queues(@NotNull @Valid QueueTopology stockReserve) {
    }

    /** Una cola con su dead-letter-exchange y su dead-letter-queue. */
    public record QueueTopology(@NotBlank String name, @NotBlank String dlx, @NotBlank String dlq) {
    }
}
