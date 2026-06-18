package com.icop.order.service;

import com.icop.order.dto.CreateOrderRequest;
import com.icop.order.entity.Order;
import com.icop.order.entity.OrderStatus;
import com.icop.order.kafka.OrderEventProducer;
import com.icop.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderEventProducer eventProducer;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_shouldSaveAndPublishEvent() {
        CreateOrderRequest request = new CreateOrderRequest(
                UUID.randomUUID(), "Laptop", 1, BigDecimal.valueOf(999.99)
        );

        Order savedOrder = new Order();
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        doNothing().when(eventProducer).publishOrderEvent(any());

        assertDoesNotThrow(() -> orderService.createOrder(request));
        verify(orderRepository).save(any(Order.class));
        verify(eventProducer).publishOrderEvent(any());
    }

    @Test
    void cancelOrder_shouldThrowIfNotPending() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () -> orderService.cancelOrder(orderId));
    }
}
