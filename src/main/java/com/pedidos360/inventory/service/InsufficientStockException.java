package com.pedidos360.inventory.service;

/** El movimiento de salida dejaria el stock en negativo. -> HTTP 422. */
public class InsufficientStockException extends InventoryException {

    public InsufficientStockException(String sku, int stock, int requested) {
        super("Stock insuficiente de '" + sku + "': hay " + stock + " y se pidieron " + requested);
    }
}
