package com.pedidos360.kafkaadmin.broker;

/** Error hablando con el cluster de Kafka (AdminClient). */
public class KafkaAdminException extends RuntimeException {

    public enum Kind {
        /** El topico, config o consumer group pedido no existe. */
        NOT_FOUND,
        /** Ya existe (crear un topico duplicado). */
        CONFLICT,
        /** Parametro invalido para el broker (ej. replication factor mayor a brokers disponibles). */
        BAD_REQUEST,
        /** Timeout, broker caido, o cualquier otro fallo de infraestructura. */
        UPSTREAM
    }

    private final Kind kind;

    public KafkaAdminException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public KafkaAdminException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}
