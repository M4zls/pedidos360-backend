package com.pedidos360.inventory.messaging.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.pedidos360.inventory.messaging.event.OrderCreatedEvent;
import com.pedidos360.inventory.service.InventoryService;
import com.pedidos360.inventory.service.InventoryService.OrderLineQuantity;

/**
 * Caso de uso del consumidor: descuenta el stock reservado por un pedido
 * recien creado. No reimplementa reglas de negocio, solo traduce el evento y
 * delega en {@link InventoryService} (que ya sabe validar stock y dejar
 * auditoria).
 */
@Service
public class ReserveStockForOrder {

    private final InventoryService inventoryService;

    public ReserveStockForOrder(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    public void execute(OrderCreatedEvent event) {
        List<OrderLineQuantity> lines = event.lines().stream()
                .map(l -> new OrderLineQuantity(l.productId(), l.quantity()))
                .toList();
        inventoryService.reserveStockForOrder(event.orderId(), lines);
    }
}
