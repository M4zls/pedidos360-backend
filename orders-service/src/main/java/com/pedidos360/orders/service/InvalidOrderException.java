package com.pedidos360.orders.service;

/** Pedido mal formado o transicion de estado no permitida (HTTP 422). */
public class InvalidOrderException extends OrderException {

    public InvalidOrderException(String message) {
        super(message);
    }
}
