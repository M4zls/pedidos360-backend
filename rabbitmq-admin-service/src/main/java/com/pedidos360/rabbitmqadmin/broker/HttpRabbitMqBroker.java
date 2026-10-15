package com.pedidos360.rabbitmqadmin.broker;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.pedidos360.rabbitmqadmin.config.RabbitMqManagementProperties;

/** Implementacion del broker contra la Management HTTP API de RabbitMQ. */
@Component
public class HttpRabbitMqBroker implements RabbitMqBroker {

    private static final Logger log = LoggerFactory.getLogger(HttpRabbitMqBroker.class);

    private final RestClient restClient;
    private final String vhost;

    public HttpRabbitMqBroker(RestClient rabbitMqManagementRestClient, RabbitMqManagementProperties properties) {
        this.restClient = rabbitMqManagementRestClient;
        this.vhost = URLEncoder.encode(properties.vhost(), StandardCharsets.UTF_8);
    }

    @Override
    public void createExchange(String name, String type, boolean durable, boolean autoDelete) {
        Map<String, Object> body = Map.of(
                "type", type,
                "durable", durable,
                "auto_delete", autoDelete);
        execute(() -> restClient.put()
                .uri("/exchanges/{vhost}/{name}", vhost, name)
                .body(body)
                .retrieve()
                .toBodilessEntity());
        log.info("Exchange creado: name={} type={} durable={}", name, type, durable);
    }

    @Override
    public void deleteExchange(String name) {
        execute(() -> restClient.delete()
                .uri("/exchanges/{vhost}/{name}", vhost, name)
                .retrieve()
                .toBodilessEntity());
        log.info("Exchange eliminado: name={}", name);
    }

    @Override
    public void createQueue(String name, boolean durable, boolean autoDelete, Map<String, Object> arguments) {
        Map<String, Object> body = Map.of(
                "durable", durable,
                "auto_delete", autoDelete,
                "arguments", arguments == null ? Map.of() : arguments);
        execute(() -> restClient.put()
                .uri("/queues/{vhost}/{name}", vhost, name)
                .body(body)
                .retrieve()
                .toBodilessEntity());
        log.info("Cola creada: name={} durable={} arguments={}", name, durable, arguments);
    }

    @Override
    public void deleteQueue(String name) {
        execute(() -> restClient.delete()
                .uri("/queues/{vhost}/{name}", vhost, name)
                .retrieve()
                .toBodilessEntity());
        log.info("Cola eliminada: name={}", name);
    }

    @Override
    public void createBinding(String source, BindingDestinationType destinationType, String destination, String routingKey) {
        Map<String, Object> body = Map.of("routing_key", routingKey == null ? "" : routingKey);
        execute(() -> restClient.post()
                .uri("/bindings/{vhost}/e/{source}/{destType}/{destination}",
                        vhost, source, destinationType.pathSegment(), destination)
                .body(body)
                .retrieve()
                .toBodilessEntity());
        log.info("Binding creado: source={} -> {}:{} (routingKey={})", source, destinationType, destination, routingKey);
    }

    @Override
    public void deleteBinding(String source, BindingDestinationType destinationType, String destination, String routingKey) {
        String propsKey = findPropsKey(source, destinationType, destination, routingKey);
        if (propsKey == null) {
            throw new RabbitMqManagementException(
                    HttpStatusCode.valueOf(404),
                    "No existe binding %s -> %s:%s con routingKey=%s"
                            .formatted(source, destinationType, destination, routingKey));
        }
        execute(() -> restClient.delete()
                .uri("/bindings/{vhost}/e/{source}/{destType}/{destination}/{propsKey}",
                        vhost, source, destinationType.pathSegment(), destination, propsKey)
                .retrieve()
                .toBodilessEntity());
        log.info("Binding eliminado: source={} -> {}:{} (routingKey={})", source, destinationType, destination, routingKey);
    }

    @SuppressWarnings("unchecked")
    private String findPropsKey(String source, BindingDestinationType destinationType, String destination, String routingKey) {
        List<Map<String, Object>> bindings = execute(() -> restClient.get()
                .uri("/bindings/{vhost}/e/{source}/{destType}/{destination}",
                        vhost, source, destinationType.pathSegment(), destination)
                .retrieve()
                .body(List.class));
        if (bindings == null) {
            return null;
        }
        String wanted = routingKey == null ? "" : routingKey;
        return bindings.stream()
                .filter(b -> wanted.equals(b.get("routing_key")))
                .map(b -> (String) b.get("properties_key"))
                .findFirst()
                .orElse(null);
    }

    private <T> T execute(java.util.function.Supplier<T> call) {
        try {
            return call.get();
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            throw new RabbitMqManagementException(ex.getStatusCode(), ex.getResponseBodyAsString());
        } catch (Exception ex) {
            throw new RabbitMqManagementException("Error hablando con RabbitMQ Management API", ex);
        }
    }
}
