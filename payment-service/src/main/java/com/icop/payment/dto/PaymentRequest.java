package com.icop.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

// used by both entry points — the REST endpoint and the kafka consumer build
// the same request, so validation rules only live in one place
public record PaymentRequest(
        @NotNull(message = "Order ID is required") UUID orderId,
        @NotNull(message = "User ID is required") UUID userId,
        @NotNull @DecimalMin(value = "0.01", message = "Amount must be positive") BigDecimal amount
) {}
