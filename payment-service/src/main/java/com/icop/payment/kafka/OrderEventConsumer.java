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

        if ("ORDER_CREATED".equals(eventType)) {
            log.info("Received ORDER_CREATED event, initiating payment processing");
            try {
                UUID orderId = UUID.fromString((String) event.get("orderId"));
                UUID userId = UUID.fromString((String) event.get("userId"));
                BigDecimal amount = new BigDecimal(event.get("totalAmount").toString());

                paymentService.processPayment(new PaymentRequest(orderId, userId, amount));
            } catch (Exception e) {
                log.error("Error processing ORDER_CREATED event: {}", e.getMessage());
            }
        }
    }
}
