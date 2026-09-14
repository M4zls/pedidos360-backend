package com.pedidos360.orders.client;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Vista minima de un producto tal como la devuelve el servicio de inventario
 * ({@code GET /api/inventory/products/{id}}). Solo los campos que pedidos
 * necesita para validar y congelar precios.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductView(
        Long id,
        String sku,
        String name,
        BigDecimal unitPrice,
        boolean active
) {}
