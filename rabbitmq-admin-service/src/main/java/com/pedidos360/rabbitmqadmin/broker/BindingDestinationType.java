package com.pedidos360.rabbitmqadmin.broker;

/** Tipo de destino de un binding: una cola o otro exchange. */
public enum BindingDestinationType {
    QUEUE("q"),
    EXCHANGE("e");

    private final String pathSegment;

    BindingDestinationType(String pathSegment) {
        this.pathSegment = pathSegment;
    }

    public String pathSegment() {
        return pathSegment;
    }
}
