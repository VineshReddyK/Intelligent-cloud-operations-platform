package com.icop.payment.repository;

import com.icop.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

// findByOrderId does double duty: the client-facing lookup AND the
// duplicate-payment guard in processPayment
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByOrderId(UUID orderId);
}
