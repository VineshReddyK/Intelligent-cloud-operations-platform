package com.icop.payment.service;

import com.icop.payment.dto.PaymentRequest;
import com.icop.payment.dto.PaymentResponse;
import com.icop.payment.entity.Payment;
import com.icop.payment.entity.PaymentStatus;
import com.icop.payment.event.PaymentEvent;
import com.icop.payment.kafka.PaymentEventProducer;
import com.icop.payment.repository.PaymentRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer eventProducer;

    public PaymentService(PaymentRepository paymentRepository, PaymentEventProducer eventProducer) {
        this.paymentRepository = paymentRepository;
        this.eventProducer = eventProducer;
    }

    @Transactional
    @CircuitBreaker(name = "payment-processor", fallbackMethod = "paymentFallback")
    public PaymentResponse processPayment(PaymentRequest request) {
        if (paymentRepository.findByOrderId(request.orderId()).isPresent()) {
            throw new IllegalStateException("Payment already exists for order: " + request.orderId());
        }

        Payment payment = new Payment();
        payment.setOrderId(request.orderId());
        payment.setUserId(request.userId());
        payment.setAmount(request.amount());

        boolean paymentSuccess = simulatePaymentGateway(request.amount());

        if (paymentSuccess) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setProcessedAt(LocalDateTime.now());
            Payment saved = paymentRepository.save(payment);

            eventProducer.publishPaymentEvent(new PaymentEvent(
                    "PAYMENT_SUCCESS", saved.getId(), saved.getOrderId(),
                    saved.getUserId(), saved.getAmount(), "SUCCESS", null, LocalDateTime.now()
            ));

            log.info("Payment successful for order: {}", request.orderId());
            return toResponse(saved);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Insufficient funds");
            payment.setProcessedAt(LocalDateTime.now());
            Payment saved = paymentRepository.save(payment);

            eventProducer.publishPaymentEvent(new PaymentEvent(
                    "PAYMENT_FAILED", saved.getId(), saved.getOrderId(),
                    saved.getUserId(), saved.getAmount(), "FAILED", "Insufficient funds", LocalDateTime.now()
            ));

            log.warn("Payment failed for order: {}", request.orderId());
            return toResponse(saved);
        }
    }

    public PaymentResponse paymentFallback(PaymentRequest request, Exception ex) {
        log.error("Payment circuit breaker triggered for order: {}. Reason: {}", request.orderId(), ex.getMessage());
        Payment payment = new Payment();
        payment.setOrderId(request.orderId());
        payment.setUserId(request.userId());
        payment.setAmount(request.amount());
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason("Payment service unavailable — circuit breaker open");
        payment.setProcessedAt(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);
        return toResponse(saved);
    }

    public PaymentResponse getPaymentById(UUID id) {
        return paymentRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id));
    }

    public PaymentResponse getPaymentByOrderId(UUID orderId) {
        return paymentRepository.findByOrderId(orderId)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderId));
    }

    private boolean simulatePaymentGateway(java.math.BigDecimal amount) {
        return amount.compareTo(java.math.BigDecimal.valueOf(10000)) < 0;
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(), payment.getOrderId(), payment.getUserId(),
                payment.getAmount(), payment.getStatus(), payment.getFailureReason(),
                payment.getCreatedAt(), payment.getProcessedAt()
        );
    }
}
