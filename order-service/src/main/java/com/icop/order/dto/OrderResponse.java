package com.icop.order.dto;

import com.icop.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// the API's view of an order — currently mirrors the entity 1:1, but having
// the separate record means the table can change without breaking clients
public record OrderResponse(
        UUID id,
        UUID userId,
        String productName,
        Integer quantity,
        BigDecimal totalAmount,
        OrderStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
