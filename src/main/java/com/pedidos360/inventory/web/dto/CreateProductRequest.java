package com.pedidos360.inventory.web.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProductRequest(
        @NotBlank @Size(max = 40) String sku,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 60) String category,
        @NotBlank @Size(max = 20) String unit,
        @NotNull @DecimalMin("0.0") BigDecimal unitPrice,
        @Min(0) int minStock,
        @Min(0) int initialStock
) {}
