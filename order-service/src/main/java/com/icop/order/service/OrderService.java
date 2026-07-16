package com.icop.order.service;

import com.icop.order.dto.CreateOrderRequest;
import com.icop.order.dto.OrderResponse;
import com.icop.order.entity.Order;
import com.icop.order.entity.OrderStatus;
import com.icop.order.event.OrderEvent;
import com.icop.order.kafka.OrderEventProducer;
import com.icop.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Order lifecycle: create, look up, cancel. Every state change also goes out
 * on Kafka so payment-service and notification-service can react without us
 * calling them — that's the whole event-driven point of the platform.
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventProducer eventProducer;

    public OrderService(OrderRepository orderRepository, OrderEventProducer eventProducer) {
        this.orderRepository = orderRepository;
        this.eventProducer = eventProducer;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setUserId(request.userId());
        order.setProductName(request.productName());
        order.setQuantity(request.quantity());
        order.setTotalAmount(request.totalAmount());
        // everything starts PENDING — payment-service moves it along from there
        order.setStatus(OrderStatus.PENDING);

        Order saved = orderRepository.save(order);

        // publish after save so the event carries the generated id
        eventProducer.publishOrderEvent(new OrderEvent(
                "ORDER_CREATED", saved.getId(), saved.getUserId(),
                saved.getProductName(), saved.getQuantity(),
                saved.getTotalAmount(), saved.getStatus().name(),
                LocalDateTime.now()
        ));

        return toResponse(saved);
    }

    public OrderResponse getOrderById(UUID id) {
        return orderRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
    }

    public List<OrderResponse> getOrdersByUser(UUID userId) {
        // newest first — that's what every order-history screen wants
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse cancelOrder(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));

        // once payment is in flight it's too late to just flip a flag —
        // that would need a refund flow, which is a different feature
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only PENDING orders can be cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        eventProducer.publishOrderEvent(new OrderEvent(
                "ORDER_CANCELLED", saved.getId(), saved.getUserId(),
                saved.getProductName(), saved.getQuantity(),
                saved.getTotalAmount(), saved.getStatus().name(),
                LocalDateTime.now()
        ));

        return toResponse(saved);
    }

    // called by the payment-events listener — deliberately quiet if the order
    // is gone, since a missing order isn't the listener's problem to solve
    @Transactional
    public void updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(newStatus);
            orderRepository.save(order);
        });
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(), order.getUserId(), order.getProductName(),
                order.getQuantity(), order.getTotalAmount(), order.getStatus(),
                order.getCreatedAt(), order.getUpdatedAt()
        );
    }
}
