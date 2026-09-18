package com.pedidos360.orders.controller.dto;

import java.math.BigDecimal;

import com.pedidos360.orders.domain.OrderStatus;

/** Proyeccion JPQL: cantidad de pedidos y monto total por estado. */
public record StatusCount(OrderStatus status, long count, BigDecimal total) {}
