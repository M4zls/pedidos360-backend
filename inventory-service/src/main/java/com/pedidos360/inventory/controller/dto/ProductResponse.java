package com.pedidos360.inventory.controller.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.pedidos360.inventory.domain.Product;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String category,
        String unit,
        String imageUrl,
        BigDecimal unitPrice,
        int stock,
        int minStock,
        boolean active,
        boolean lowStock,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(), p.getSku(), p.getName(), p.getCategory(), p.getUnit(), p.getImageUrl(),
                p.getUnitPrice(), p.getStock(), p.getMinStock(), p.isActive(), p.isLowStock(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
