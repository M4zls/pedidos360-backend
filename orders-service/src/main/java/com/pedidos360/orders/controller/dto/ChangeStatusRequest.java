package com.pedidos360.orders.controller.dto;

import com.pedidos360.orders.domain.OrderStatus;

import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(@NotNull OrderStatus status) {}
