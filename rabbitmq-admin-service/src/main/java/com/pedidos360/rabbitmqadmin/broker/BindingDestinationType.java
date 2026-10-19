package com.pedidos360.rabbitmqadmin.broker;

/** Tipo de destino de un binding: una cola o otro exchange. */
public enum BindingDestinationType {
    QUEUE("q", "queue"),
    EXCHANGE("e", "exchange");

    private final String pathSegment;
    private final String wireValue;

    BindingDestinationType(String pathSegment, String wireValue) {
        this.pathSegment = pathSegment;
        this.wireValue = wireValue;
    }

    /** Segmento de la URL de la Management API ({@code /bindings/{vhost}/e/{source}/q/{queue}}). */
    public String pathSegment() {
        return pathSegment;
    }

    /** Valor del campo {@code destination_type} en las respuestas de la Management API. */
    public static BindingDestinationType fromWireValue(String value) {
        for (BindingDestinationType type : values()) {
            if (type.wireValue.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("destination_type desconocido: " + value);
    }
}
