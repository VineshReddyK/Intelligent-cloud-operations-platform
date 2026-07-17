package com.icop.payment.kafka;

import com.icop.payment.event.PaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes payment outcomes to payment.events — order-service listens and
 * updates order status, notification-service listens and emails the user.
 * Keyed by order id so each order's events stay in sequence.
 */
@Component
public class PaymentEventProducer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventProducer.class);
    private static final String PAYMENT_TOPIC = "payment.events";

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    public PaymentEventProducer(KafkaTemplate<String, PaymentEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPaymentEvent(PaymentEvent event) {
        log.info("Publishing payment event: {} for order: {}", event.eventType(), event.orderId());
        kafkaTemplate.send(PAYMENT_TOPIC, event.orderId().toString(), event)
                .whenComplete((result, ex) -> {
                    // log-and-continue: the payment row is already committed,
                    // a publish failure shouldn't undo it
                    if (ex != null) {
                        log.error("Failed to publish payment event: {}", ex.getMessage());
                    }
                });
    }
}
