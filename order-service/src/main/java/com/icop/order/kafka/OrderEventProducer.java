package com.icop.order.kafka;

import com.icop.order.event.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);
    private static final String ORDER_TOPIC = "order.events";

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public OrderEventProducer(KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderEvent(OrderEvent event) {
        log.info("Publishing order event: {} for order: {}", event.eventType(), event.orderId());
        kafkaTemplate.send(ORDER_TOPIC, event.orderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish order event: {}", ex.getMessage());
                    } else {
                        log.debug("Order event published to partition {}", result.getRecordMetadata().partition());
                    }
                });
    }
}
