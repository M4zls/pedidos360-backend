package com.pedidos360.inventory.controller.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos editables de un producto. El SKU y el stock no se tocan por aca. */
public record UpdateProductRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 60) String category,
        @NotBlank @Size(max = 20) String unit,
        @NotNull @DecimalMin("0.0") BigDecimal unitPrice,
        @Min(0) int minStock,
        boolean active
) {}
