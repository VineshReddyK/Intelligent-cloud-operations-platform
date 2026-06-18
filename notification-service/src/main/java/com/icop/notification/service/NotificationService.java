package com.icop.notification.service;

import com.icop.notification.event.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void handleOrderCreated(NotificationEvent event) {
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
        log.warn("[NOTIFICATION] Payment failed - OrderId: {}, UserId: {}, Reason: {}",
                event.orderId(), event.userId(), event.failureReason());
    }
}
