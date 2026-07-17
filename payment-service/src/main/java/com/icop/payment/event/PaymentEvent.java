package com.icop.payment.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// what goes out on payment.events — full snapshot, same philosophy as
// order events: consumers should never need to call back for details
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
