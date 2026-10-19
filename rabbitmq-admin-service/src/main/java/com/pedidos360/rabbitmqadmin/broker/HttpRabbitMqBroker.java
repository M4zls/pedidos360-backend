package com.pedidos360.rabbitmqadmin.broker;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import com.pedidos360.rabbitmqadmin.config.RabbitMqManagementProperties;

/** Implementacion del broker contra la Management HTTP API de RabbitMQ. */
@Component
public class HttpRabbitMqBroker implements RabbitMqBroker {

    private static final Logger log = LoggerFactory.getLogger(HttpRabbitMqBroker.class);

    private static final ParameterizedTypeReference<List<Map<String, Object>>> JSON_LIST =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    /** Sin encodear: RestClient encodea cada variable de la URI ("/" -> "%2F"). */
    private final String vhost;

    public HttpRabbitMqBroker(RestClient rabbitMqManagementRestClient, RabbitMqManagementProperties properties) {
        this.restClient = rabbitMqManagementRestClient;
        this.vhost = properties.vhost();
    }

    // ---- exchanges ----

    @Override
    public List<ExchangeInfo> listExchanges() {
        List<Map<String, Object>> exchanges = execute(() -> restClient.get()
                .uri("/exchanges/{vhost}", vhost)
                .retrieve()
                .body(JSON_LIST));
        // El exchange por defecto ("") existe siempre y no se puede administrar.
        return exchanges.stream()
                .filter(e -> !((String) e.get("name")).isEmpty())
                .map(HttpRabbitMqBroker::toExchangeInfo)
                .toList();
    }

    @Override
    public ExchangeInfo getExchange(String name) {
        return toExchangeInfo(execute(() -> restClient.get()
                .uri("/exchanges/{vhost}/{name}", vhost, name)
                .retrieve()
                .body(JSON_OBJECT)));
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

    // ---- colas ----

    @Override
    public List<QueueInfo> listQueues() {
        List<Map<String, Object>> queues = execute(() -> restClient.get()
                .uri("/queues/{vhost}", vhost)
                .retrieve()
                .body(JSON_LIST));
        return queues.stream().map(HttpRabbitMqBroker::toQueueInfo).toList();
    }

    @Override
    public QueueInfo getQueue(String name) {
        return toQueueInfo(execute(() -> restClient.get()
                .uri("/queues/{vhost}/{name}", vhost, name)
                .retrieve()
                .body(JSON_OBJECT)));
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

    // ---- bindings ----

    @Override
    public List<BindingInfo> listBindings() {
        List<Map<String, Object>> bindings = execute(() -> restClient.get()
                .uri("/bindings/{vhost}", vhost)
                .retrieve()
                .body(JSON_LIST));
        // Los bindings implicitos del exchange por defecto ("") no se administran.
        return bindings.stream()
                .filter(b -> !((String) b.get("source")).isEmpty())
                .map(b -> new BindingInfo(
                        (String) b.get("source"),
                        BindingDestinationType.fromWireValue((String) b.get("destination_type")),
                        (String) b.get("destination"),
                        (String) b.get("routing_key")))
                .toList();
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

    private String findPropsKey(String source, BindingDestinationType destinationType, String destination, String routingKey) {
        List<Map<String, Object>> bindings = execute(() -> restClient.get()
                .uri("/bindings/{vhost}/e/{source}/{destType}/{destination}",
                        vhost, source, destinationType.pathSegment(), destination)
                .retrieve()
                .body(JSON_LIST));
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

    private static ExchangeInfo toExchangeInfo(Map<String, Object> e) {
        return new ExchangeInfo(
                (String) e.get("name"),
                (String) e.get("type"),
                Boolean.TRUE.equals(e.get("durable")),
                Boolean.TRUE.equals(e.get("auto_delete")));
    }

    @SuppressWarnings("unchecked")
    private static QueueInfo toQueueInfo(Map<String, Object> q) {
        return new QueueInfo(
                (String) q.get("name"),
                Boolean.TRUE.equals(q.get("durable")),
                Boolean.TRUE.equals(q.get("auto_delete")),
                // Recien creada, el broker todavia no reporta estadisticas.
                q.get("messages") instanceof Number n ? n.longValue() : 0,
                q.get("consumers") instanceof Number n ? n.longValue() : 0,
                q.get("arguments") instanceof Map<?, ?> args ? (Map<String, Object>) args : Map.of());
    }

    private <T> T execute(Supplier<T> call) {
        try {
            return call.get();
        } catch (HttpStatusCodeException ex) {
            throw new RabbitMqManagementException(ex.getStatusCode(), ex.getResponseBodyAsString());
        } catch (RabbitMqManagementException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RabbitMqManagementException("Error hablando con RabbitMQ Management API", ex);
        }
    }
}
