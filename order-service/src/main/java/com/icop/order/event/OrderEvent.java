package com.icop.order.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderEvent(
        String eventType,
        UUID orderId,
        UUID userId,
        String productName,
        Integer quantity,
        BigDecimal totalAmount,
        String status,
        LocalDateTime timestamp
) {}
