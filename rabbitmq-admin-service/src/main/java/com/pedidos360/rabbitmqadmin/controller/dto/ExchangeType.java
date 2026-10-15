package com.pedidos360.rabbitmqadmin.controller.dto;

/** Tipos de exchange soportados por el administrador. */
public enum ExchangeType {
    DIRECT,
    TOPIC,
    FANOUT,
    HEADERS;

    public String wireValue() {
        return name().toLowerCase();
    }
}
