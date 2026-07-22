package com.icop.notification.service;

import com.icop.notification.event.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Turns platform events into user notifications. Right now "notification"
 * means a structured log line — the seam is deliberately here, though: swap
 * these bodies for an email/SMS/push call and nothing upstream has to change.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void handleOrderCreated(NotificationEvent event) {
        // in a real build this would be the "thanks for your order" email
        log.info("[NOTIFICATION] Order created - OrderId: {}, UserId: {}, Amount: ${}",
                event.orderId(), event.userId(), event.amount());
    }

    public void handleOrderCancelled(NotificationEvent event) {
        log.info("[NOTIFICATION] Order cancelled - OrderId: {}, UserId: {}",
                event.orderId(), event.userId());
    }

    public void handlePaymentSuccess(NotificationEvent event) {
        log.info("[NOTIFICATION] Payment successful - OrderId: {}, UserId: {}, Amount: ${}",
                event.orderId(), event.userId(), event.amount());
    }

    public void handlePaymentFailed(NotificationEvent event) {
        // warn, not info — a failed payment is the one a human might actually
        // want to page on
        log.warn("[NOTIFICATION] Payment failed - OrderId: {}, UserId: {}, Reason: {}",
                event.orderId(), event.userId(), event.failureReason());
    }
}
