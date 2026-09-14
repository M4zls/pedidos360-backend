package com.pedidos360.orders.controller.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.pedidos360.orders.domain.CustomerOrder;
import com.pedidos360.orders.domain.OrderStatus;

public record OrderResponse(
        Long id,
        String customerEmail,
        String customerName,
        OrderStatus status,
        String note,
        BigDecimal total,
        List<Line> lines,
        Instant createdAt,
        Instant updatedAt
) {
    public record Line(
            Long productId,
            String sku,
            String name,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal subtotal
    ) {}

    public static OrderResponse from(CustomerOrder o) {
        List<Line> lines = o.getLines().stream()
                .map(l -> new Line(l.getProductId(), l.getSku(), l.getName(),
                        l.getUnitPrice(), l.getQuantity(), l.getSubtotal()))
                .toList();
        return new OrderResponse(
                o.getId(), o.getCustomerEmail(), o.getCustomerName(), o.getStatus(),
                o.getNote(), o.getTotal(), lines, o.getCreatedAt(), o.getUpdatedAt());
    }
}
