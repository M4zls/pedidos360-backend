package com.pedidos360.inventory.controller.dto;

import java.time.Instant;

import com.pedidos360.inventory.domain.StockAlert;

public record StockAlertResponse(
        Long id,
        Long productId,
        String productName,
        String message,
        boolean read,
        Instant createdAt
) {
    public static StockAlertResponse from(StockAlert a) {
        return new StockAlertResponse(
                a.getId(),
                a.getProduct().getId(),
                a.getProduct().getName(),
                a.getMessage(),
                a.isRead(),
                a.getCreatedAt());
    }
}
