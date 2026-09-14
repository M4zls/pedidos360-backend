package com.pedidos360.orders.service;

/** Base de los errores de negocio de pedidos. El handler web los mapea a HTTP. */
public abstract class OrderException extends RuntimeException {

    protected OrderException(String message) {
        super(message);
    }
}
