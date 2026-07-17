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

/**
 * Payment processing behind a resilience4j circuit breaker. When the
 * (simulated) gateway starts failing repeatedly, the breaker opens and
 * requests short-circuit to the fallback instead of piling up threads
 * against a dead dependency — fail fast, recover gracefully.
 */
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
        // one payment per order, ever. the unique constraint on orderId backs
        // this up at the DB level, but checking first gives a better error
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

            // order-service listens for this and flips the order status
            eventProducer.publishPaymentEvent(new PaymentEvent(
                    "PAYMENT_SUCCESS", saved.getId(), saved.getOrderId(),
                    saved.getUserId(), saved.getAmount(), "SUCCESS", null, LocalDateTime.now()
            ));

            log.info("Payment successful for order: {}", request.orderId());
            return toResponse(saved);
        } else {
            // failed payments get persisted too — the audit trail matters as
            // much for declines as for successes
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

    // circuit-breaker fallback — same signature plus the exception, per
    // resilience4j's contract. records the failure so nothing goes missing
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

    // stand-in for a real gateway: anything under 10k clears, the rest
    // declines. deterministic on purpose so the demo (and tests) behave
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
