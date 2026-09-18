package com.pedidos360.inventory.controller;

import java.util.List;

import com.pedidos360.inventory.controller.dto.StockAlertResponse;
import com.pedidos360.inventory.service.InventoryService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Alertas automaticas de stock bajo (se generan solas al cruzar el umbral). */
@RestController
@RequestMapping("/api/inventory/alerts")
public class StockAlertController {

    private final InventoryService inventory;

    public StockAlertController(InventoryService inventory) {
        this.inventory = inventory;
    }

    @GetMapping
    public List<StockAlertResponse> list() {
        return inventory.alerts().stream().map(StockAlertResponse::from).toList();
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        inventory.markAlertRead(id);
        return ResponseEntity.noContent().build();
    }
}
