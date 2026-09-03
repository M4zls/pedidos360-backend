package com.pedidos360.inventory.service;

/** No existe el producto pedido. -> HTTP 404. */
public class ProductNotFoundException extends InventoryException {

    public ProductNotFoundException(Long id) {
        super("No existe el producto con id " + id);
    }
}
