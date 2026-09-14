package com.pedidos360.inventory.controller.dto;

import com.pedidos360.inventory.domain.MovementType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta de un movimiento de stock.
 * - IN / OUT: {@code quantity} es cuanto entra o sale (> 0).
 * - ADJUSTMENT: {@code quantity} es el stock final que queda (>= 0).
 */
public record StockMovementRequest(
        @NotNull MovementType type,
        @Min(0) int quantity,
        @Size(max = 200) String reason
) {}
