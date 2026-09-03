package com.pedidos360.inventory.controller;

import java.util.List;

import com.pedidos360.inventory.domain.StockMovement;
import com.pedidos360.inventory.service.InventoryService;
import com.pedidos360.inventory.controller.dto.StockMovementRequest;
import com.pedidos360.inventory.controller.dto.StockMovementResponse;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Entradas, salidas y ajustes de stock de un producto, con su historial. */
@RestController
@RequestMapping("/api/inventory/products/{productId}/movements")
public class StockMovementController {

    private final InventoryService inventory;

    public StockMovementController(InventoryService inventory) {
        this.inventory = inventory;
    }

    @GetMapping
    public List<StockMovementResponse> history(@PathVariable Long productId) {
        return inventory.history(productId).stream().map(StockMovementResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StockMovementResponse register(@PathVariable Long productId,
                                          @Valid @RequestBody StockMovementRequest body,
                                          @AuthenticationPrincipal Jwt jwt) {
        String createdBy = jwt != null ? jwt.getSubject() : null;
        StockMovement movement = inventory.registerMovement(
                productId, body.type(), body.quantity(), body.reason(), createdBy);
        return StockMovementResponse.from(movement);
    }
}
