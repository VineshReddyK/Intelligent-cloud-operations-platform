package com.icop.notification.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// a unified shape for both order and payment events — the consumer flattens
// each topic's message into this so the handlers don't care where it came from
public record NotificationEvent(
        String eventType,
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        String status,
        String failureReason,
        LocalDateTime timestamp
) {}
