package com.icop.payment.service;

import com.icop.payment.dto.PaymentRequest;
import com.icop.payment.dto.PaymentResponse;
import com.icop.payment.entity.Payment;
import com.icop.payment.entity.PaymentStatus;
import com.icop.payment.kafka.PaymentEventProducer;
import com.icop.payment.repository.PaymentRepository;
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
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentEventProducer eventProducer;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void processPayment_shouldSucceedForAmountUnder10000() {
        UUID orderId = UUID.randomUUID();
        PaymentRequest request = new PaymentRequest(orderId, UUID.randomUUID(), BigDecimal.valueOf(500));

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(eventProducer).publishPaymentEvent(any());

        PaymentResponse response = paymentService.processPayment(request);

        assertEquals(PaymentStatus.SUCCESS, response.status());
    }

    @Test
    void processPayment_shouldFailForDuplicateOrder() {
        UUID orderId = UUID.randomUUID();
        PaymentRequest request = new PaymentRequest(orderId, UUID.randomUUID(), BigDecimal.valueOf(100));

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(new Payment()));

        assertThrows(IllegalStateException.class, () -> paymentService.processPayment(request));
    }

    @Test
    void processPayment_shouldFailForAmountOver10000() {
        UUID orderId = UUID.randomUUID();
        PaymentRequest request = new PaymentRequest(orderId, UUID.randomUUID(), BigDecimal.valueOf(15000));

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(eventProducer).publishPaymentEvent(any());

        PaymentResponse response = paymentService.processPayment(request);

        assertEquals(PaymentStatus.FAILED, response.status());
        assertEquals("Insufficient funds", response.failureReason());
    }

    @Test
    void getPaymentByOrderId_throwsWhenNotFound() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> paymentService.getPaymentByOrderId(orderId));
    }
}
