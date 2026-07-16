package com.icop.order.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// the message that goes out on order.events. it carries the full order
// snapshot — fatter than just ids, but consumers never need a lookup call
// back to us, which is the point
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
