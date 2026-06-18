package com.icop.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull(message = "User ID is required") UUID userId,
        @NotBlank(message = "Product name is required") String productName,
        @NotNull @Min(value = 1, message = "Quantity must be at least 1") Integer quantity,
        @NotNull @DecimalMin(value = "0.01", message = "Amount must be positive") BigDecimal totalAmount
) {}
