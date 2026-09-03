package com.pedidos360.inventory.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.pedidos360.inventory.domain.Product;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String category,
        String unit,
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
                p.getId(), p.getSku(), p.getName(), p.getCategory(), p.getUnit(),
                p.getUnitPrice(), p.getStock(), p.getMinStock(), p.isActive(), p.isLowStock(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
