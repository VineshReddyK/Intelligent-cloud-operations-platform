package com.icop.notification.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationEvent(
        String eventType,
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        String status,
        String failureReason,
        LocalDateTime timestamp
) {}
