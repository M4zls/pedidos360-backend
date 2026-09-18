package com.pedidos360.orders.controller.dto;

import java.time.Instant;

import com.pedidos360.orders.domain.OrderNotification;

public record OrderNotificationResponse(
        Long id,
        Long orderId,
        String message,
        boolean read,
        Instant createdAt
) {
    public static OrderNotificationResponse from(OrderNotification n) {
        return new OrderNotificationResponse(
                n.getId(), n.getOrderId(), n.getMessage(), n.isRead(), n.getCreatedAt());
    }
}
