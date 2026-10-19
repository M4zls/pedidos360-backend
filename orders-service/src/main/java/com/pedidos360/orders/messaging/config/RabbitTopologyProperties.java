package com.pedidos360.orders.messaging.config;

import java.util.Locale;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Nombres de exchanges, colas y routing keys del dominio de pedidos, leidos de
 * {@code app.rabbitmq} en application.yml. Configuracion centralizada: todo el
 * codigo de mensajeria sale de aca, nunca de un literal suelto. Si falta
 * algun nombre, el servicio no arranca.
 */
@Validated
@ConfigurationProperties(prefix = "app.rabbitmq")
public record RabbitTopologyProperties(
        @NotNull @Valid Exchanges exchanges,
        @NotNull @Valid RoutingKeys routingKeys,
        @NotNull @Valid Queues queues) {

    /**
     * @param ordersDirect      direct: eventos puntuales del ciclo de vida de un pedido
     * @param ordersStatusTopic topic: cada cambio de estado, para quien se quiera suscribir
     */
    public record Exchanges(@NotBlank String ordersDirect, @NotBlank String ordersStatusTopic) {
    }

    /**
     * @param orderCreated      se publica una vez, al crear el pedido
     * @param orderStatusPrefix prefijo de {@code <prefijo><estado>} en el exchange topic
     */
    public record RoutingKeys(@NotBlank String orderCreated, @NotBlank String orderStatusPrefix) {
    }

    /**
     * @param ordersNotifications cola que consume orders-service para avisarle al cliente
     * @param dispatchReady       cola que consume orders-service para avisarle a despacho
     */
    public record Queues(@NotNull @Valid QueueTopology ordersNotifications, @NotNull @Valid QueueTopology dispatchReady) {
    }

    /** Una cola con su dead-letter-exchange y su dead-letter-queue. */
    public record QueueTopology(@NotBlank String name, @NotBlank String dlx, @NotBlank String dlq) {
    }

    /** Routing key del exchange topic para un estado: {@code order.status.listo}, etc. */
    public String statusRoutingKey(String statusName) {
        return routingKeys.orderStatusPrefix() + statusName.toLowerCase(Locale.ROOT);
    }
}
