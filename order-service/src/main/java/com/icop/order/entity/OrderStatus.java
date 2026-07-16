package com.icop.order.entity;

// the full lifecycle an order can walk through. payment-service drives the
// PAYMENT_* transitions via kafka events; SHIPPED/DELIVERED are there for
// the day a fulfillment service exists
public enum OrderStatus {
    PENDING, CONFIRMED, PAYMENT_PROCESSING, PAYMENT_SUCCESS, PAYMENT_FAILED, CANCELLED, SHIPPED, DELIVERED
}
