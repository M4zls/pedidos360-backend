package com.pedidos360.rabbitmqadmin.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.pedidos360.rabbitmqadmin.broker.BindingDestinationType;
import com.pedidos360.rabbitmqadmin.broker.BindingInfo;
import com.pedidos360.rabbitmqadmin.broker.ExchangeInfo;
import com.pedidos360.rabbitmqadmin.broker.QueueInfo;
import com.pedidos360.rabbitmqadmin.broker.RabbitMqBroker;

/**
 * Logica de administracion de topologia: crear, consultar y borrar
 * exchanges, colas y bindings. El controller solo traduce HTTP; las reglas
 * (no pisar un recurso existente, que la DLX exista, armar los x-arguments)
 * viven aca. Habla con el broker a traves de {@link RabbitMqBroker}.
 */
@Service
public class RabbitAdminService {

    private final RabbitMqBroker broker;

    public RabbitAdminService(RabbitMqBroker broker) {
        this.broker = broker;
    }

    // ---- exchanges ----

    public List<ExchangeInfo> listExchanges() {
        return broker.listExchanges();
    }

    public ExchangeInfo getExchange(String name) {
        return broker.getExchange(name);
    }

    public void createExchange(String name, String type, boolean durable, boolean autoDelete) {
        if (exchangeExists(name)) {
            throw new TopologyConflictException("El exchange '%s' ya existe".formatted(name));
        }
        broker.createExchange(name, type, durable, autoDelete);
    }

    public void deleteExchange(String name) {
        broker.deleteExchange(name);
    }

    // ---- colas ----

    public List<QueueInfo> listQueues() {
        return broker.listQueues();
    }

    public QueueInfo getQueue(String name) {
        return broker.getQueue(name);
    }

    /**
     * Crea una cola. Si se indica una dead-letter-exchange, los mensajes
     * rechazados o no entregados (nack sin requeue, TTL vencido) van a parar
     * ahi en vez de perderse; por eso tiene que existir de antemano.
     */
    public void createQueue(String name, boolean durable, boolean autoDelete,
            String deadLetterExchange, String deadLetterRoutingKey) {
        if (queueExists(name)) {
            throw new TopologyConflictException("La cola '%s' ya existe".formatted(name));
        }
        Map<String, Object> arguments = new HashMap<>();
        if (StringUtils.hasText(deadLetterExchange)) {
            if (!exchangeExists(deadLetterExchange)) {
                throw new InvalidTopologyException(
                        "La dead-letter-exchange '%s' no existe; creala antes que la cola".formatted(deadLetterExchange));
            }
            arguments.put("x-dead-letter-exchange", deadLetterExchange);
            if (StringUtils.hasText(deadLetterRoutingKey)) {
                arguments.put("x-dead-letter-routing-key", deadLetterRoutingKey);
            }
        }
        broker.createQueue(name, durable, autoDelete, arguments);
    }

    public void deleteQueue(String name) {
        broker.deleteQueue(name);
    }

    // ---- bindings ----

    public List<BindingInfo> listBindings() {
        return broker.listBindings();
    }

    public void createBinding(String source, BindingDestinationType destinationType, String destination, String routingKey) {
        if (!exchangeExists(source)) {
            throw new InvalidTopologyException("El exchange origen '%s' no existe".formatted(source));
        }
        boolean destinationExists = destinationType == BindingDestinationType.QUEUE
                ? queueExists(destination)
                : exchangeExists(destination);
        if (!destinationExists) {
            throw new InvalidTopologyException("El destino %s '%s' no existe"
                    .formatted(destinationType == BindingDestinationType.QUEUE ? "cola" : "exchange", destination));
        }
        broker.createBinding(source, destinationType, destination, routingKey);
    }

    public void deleteBinding(String source, BindingDestinationType destinationType, String destination, String routingKey) {
        broker.deleteBinding(source, destinationType, destination, routingKey);
    }

    private boolean exchangeExists(String name) {
        return broker.listExchanges().stream().anyMatch(e -> e.name().equals(name));
    }

    private boolean queueExists(String name) {
        return broker.listQueues().stream().anyMatch(q -> q.name().equals(name));
    }
}
