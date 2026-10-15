package com.pedidos360.rabbitmqadmin.broker;

import org.springframework.http.HttpStatusCode;

/** Error al hablar con la Management HTTP API de RabbitMQ. */
public class RabbitMqManagementException extends RuntimeException {

    private final HttpStatusCode status;

    public RabbitMqManagementException(HttpStatusCode status, String message) {
        super(message);
        this.status = status;
    }

    public RabbitMqManagementException(String message, Throwable cause) {
        super(message, cause);
        this.status = null;
    }

    public HttpStatusCode status() {
        return status;
    }
}
