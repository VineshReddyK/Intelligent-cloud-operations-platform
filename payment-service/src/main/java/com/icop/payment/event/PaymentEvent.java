package com.icop.payment.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentEvent(
        String eventType,
        UUID paymentId,
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        String status,
        String failureReason,
        LocalDateTime timestamp
) {}
