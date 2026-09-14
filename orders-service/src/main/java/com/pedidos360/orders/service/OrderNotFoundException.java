package com.pedidos360.orders.service;

public class OrderNotFoundException extends OrderException {

    public OrderNotFoundException(Long id) {
        super("No existe el pedido " + id);
    }
}
