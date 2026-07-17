package com.icop.payment.dto;

import com.icop.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// full payment view including failureReason — clients need to know *why*
// a payment bounced, not just that it did
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
