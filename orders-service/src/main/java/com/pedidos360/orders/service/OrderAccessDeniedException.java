package com.pedidos360.orders.service;

/** El usuario no es dueño del pedido ni staff (HTTP 403). */
public class OrderAccessDeniedException extends OrderException {

    public OrderAccessDeniedException() {
        super("No tenes permiso sobre este pedido");
    }
}
