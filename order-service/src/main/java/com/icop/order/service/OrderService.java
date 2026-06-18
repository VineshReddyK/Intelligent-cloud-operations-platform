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
        order.setStatus(OrderStatus.PENDING);

        Order saved = orderRepository.save(order);

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
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse cancelOrder(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));

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
