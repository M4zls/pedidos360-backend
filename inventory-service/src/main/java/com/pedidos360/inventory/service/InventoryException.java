package com.pedidos360.inventory.service;

/** Base de los errores de negocio del inventario. El handler web los mapea a HTTP. */
public abstract class InventoryException extends RuntimeException {

    protected InventoryException(String message) {
        super(message);
    }
}
