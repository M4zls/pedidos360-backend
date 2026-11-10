package com.pedidos360.kafkaadmin.service;

/** Se intento crear un topico que ya existe. */
public class TopologyConflictException extends RuntimeException {

    public TopologyConflictException(String message) {
        super(message);
    }
}
