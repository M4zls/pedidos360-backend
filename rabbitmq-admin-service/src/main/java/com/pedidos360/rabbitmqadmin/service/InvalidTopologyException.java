package com.pedidos360.rabbitmqadmin.service;

/**
 * El pedido es valido en forma pero no tiene sentido contra la topologia
 * actual (ej. una dead-letter-exchange que no existe). Se responde 400.
 */
public class InvalidTopologyException extends RuntimeException {

    public InvalidTopologyException(String message) {
        super(message);
    }
}
