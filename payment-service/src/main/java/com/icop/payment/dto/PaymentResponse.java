package com.icop.payment.dto;

import com.icop.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        PaymentStatus status,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime processedAt
) {}
