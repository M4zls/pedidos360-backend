package com.pedidos360.rabbitmqadmin.service;

/** El recurso que se quiere crear ya existe. Se responde 409. */
public class TopologyConflictException extends RuntimeException {

    public TopologyConflictException(String message) {
        super(message);
    }
}
