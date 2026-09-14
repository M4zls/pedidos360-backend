package com.pedidos360.orders.client;

/** El servicio de inventario no respondio o fallo (distinto de "producto no existe"). */
public class InventoryUnavailableException extends RuntimeException {

    public InventoryUnavailableException(Throwable cause) {
        super("No se pudo consultar el inventario", cause);
    }
}
