package com.pedidos360.inventory.messaging.consumer;

/** Un mensaje no se pudo procesar (payload invalido o fallo de negocio); va a la DLQ. */
public class MessageProcessingException extends RuntimeException {

    public MessageProcessingException(String message) {
        super(message);
    }

    public MessageProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
