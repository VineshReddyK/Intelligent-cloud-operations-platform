package com.icop.notification.kafka;

import com.icop.notification.event.NotificationEvent;
import com.icop.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * The pure consumer at the end of the chain — it listens to both order and
 * payment topics and never publishes anything of its own. Two separate
 * consumer groups so notification lag can't slow down (or get tangled with)
 * the payment flow reading the same order.events topic.
 */
@Component
public class NotificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);

    private final NotificationService notificationService;

    public NotificationEventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "order.events", groupId = "notification-order-group")
    public void handleOrderEvent(Map<String, Object> raw) {
        try {
            NotificationEvent event = toEvent(raw);
            switch (event.eventType()) {
                case "ORDER_CREATED" -> notificationService.handleOrderCreated(event);
                case "ORDER_CANCELLED" -> notificationService.handleOrderCancelled(event);
                // this topic carries the whole order lifecycle; we only care
                // about a couple of its event types
                default -> log.debug("Unhandled order event type: {}", event.eventType());
            }
        } catch (Exception e) {
            // never rethrow — a notification is best-effort, and a poison
            // message shouldn't wedge the consumer in a redelivery loop
            log.error("Error processing order event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "payment.events", groupId = "notification-payment-group")
    public void handlePaymentEvent(Map<String, Object> raw) {
        try {
            NotificationEvent event = toEvent(raw);
            switch (event.eventType()) {
                case "PAYMENT_SUCCESS" -> notificationService.handlePaymentSuccess(event);
                case "PAYMENT_FAILED" -> notificationService.handlePaymentFailed(event);
                default -> log.debug("Unhandled payment event type: {}", event.eventType());
            }
        } catch (Exception e) {
            log.error("Error processing payment event: {}", e.getMessage());
        }
    }

    // order and payment events have slightly different shapes — order calls it
    // "totalAmount", payment calls it "amount" — so normalize both into one
    // NotificationEvent here and let the handlers stay simple
    private NotificationEvent toEvent(Map<String, Object> raw) {
        return new NotificationEvent(
                (String) raw.get("eventType"),
                raw.get("orderId") != null ? UUID.fromString((String) raw.get("orderId")) : null,
                raw.get("userId") != null ? UUID.fromString((String) raw.get("userId")) : null,
                raw.get("totalAmount") != null ? new BigDecimal(raw.get("totalAmount").toString())
                        : raw.get("amount") != null ? new BigDecimal(raw.get("amount").toString()) : BigDecimal.ZERO,
                (String) raw.get("status"),
                (String) raw.get("failureReason"),
                null
        );
    }
}
