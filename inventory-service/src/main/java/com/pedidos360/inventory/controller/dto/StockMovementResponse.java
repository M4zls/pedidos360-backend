package com.pedidos360.inventory.controller.dto;

import java.time.Instant;

import com.pedidos360.inventory.domain.MovementType;
import com.pedidos360.inventory.domain.StockMovement;

public record StockMovementResponse(
        Long id,
        Long productId,
        String productSku,
        MovementType type,
        int quantity,
        int resultingStock,
        String reason,
        String createdBy,
        Instant createdAt
) {
    public static StockMovementResponse from(StockMovement m) {
        return new StockMovementResponse(
                m.getId(),
                m.getProduct().getId(),
                m.getProduct().getSku(),
                m.getType(),
                m.getQuantity(),
                m.getResultingStock(),
                m.getReason(),
                m.getCreatedBy(),
                m.getCreatedAt());
    }
}
