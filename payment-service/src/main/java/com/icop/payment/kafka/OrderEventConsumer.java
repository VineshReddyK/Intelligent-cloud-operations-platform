package com.icop.payment.kafka;

import com.icop.payment.dto.PaymentRequest;
import com.icop.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * The other half of the order → payment choreography: order-service publishes
 * ORDER_CREATED, we pick it up here and kick off payment automatically.
 * No REST call between the two services anywhere.
 *
 * Events arrive as a plain Map (see KafkaConfig) so we're not coupled to
 * order-service's event class — just its JSON field names.
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final PaymentService paymentService;

    public OrderEventConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = "order.events", groupId = "payment-service-group")
    public void handleOrderEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");

        // the topic carries the whole order lifecycle; only creation starts a payment
        if ("ORDER_CREATED".equals(eventType)) {
            log.info("Received ORDER_CREATED event, initiating payment processing");
            try {
                UUID orderId = UUID.fromString((String) event.get("orderId"));
                UUID userId = UUID.fromString((String) event.get("userId"));
                BigDecimal amount = new BigDecimal(event.get("totalAmount").toString());

                paymentService.processPayment(new PaymentRequest(orderId, userId, amount));
            } catch (Exception e) {
                // swallow rather than rethrow — an endless redelivery loop on a
                // poison message would be worse than one lost payment attempt
                log.error("Error processing ORDER_CREATED event: {}", e.getMessage());
            }
        }
    }
}
