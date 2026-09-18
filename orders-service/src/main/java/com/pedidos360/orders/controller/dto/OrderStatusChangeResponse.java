package com.pedidos360.orders.controller.dto;

import java.time.Instant;

import com.pedidos360.orders.domain.OrderStatus;
import com.pedidos360.orders.domain.OrderStatusChange;

public record OrderStatusChangeResponse(
        Long id,
        OrderStatus fromStatus,
        OrderStatus toStatus,
        String changedBy,
        Instant changedAt
) {
    public static OrderStatusChangeResponse from(OrderStatusChange c) {
        return new OrderStatusChangeResponse(
                c.getId(), c.getFromStatus(), c.getToStatus(), c.getChangedBy(), c.getChangedAt());
    }
}
