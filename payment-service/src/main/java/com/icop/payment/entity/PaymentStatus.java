package com.icop.payment.entity;

// REFUNDED is reserved for the future refund flow — nothing sets it yet,
// but having it in the enum means no migration when that lands
public enum PaymentStatus {
    PENDING, SUCCESS, FAILED, REFUNDED
}
