package com.pedidos360.rabbitmqadmin.broker;

import java.util.Map;

/**
 * Una cola tal como la reporta el broker.
 *
 * @param messages  mensajes en la cola (listos + sin ack)
 * @param consumers consumidores conectados
 * @param arguments x-arguments (ej. x-dead-letter-exchange)
 */
public record QueueInfo(
        String name,
        boolean durable,
        boolean autoDelete,
        long messages,
        long consumers,
        Map<String, Object> arguments) {
}
