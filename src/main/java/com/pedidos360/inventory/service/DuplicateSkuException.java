package com.pedidos360.inventory.service;

/** Ya hay un producto con ese SKU. -> HTTP 409. */
public class DuplicateSkuException extends InventoryException {

    public DuplicateSkuException(String sku) {
        super("Ya existe un producto con el SKU '" + sku + "'");
    }
}
