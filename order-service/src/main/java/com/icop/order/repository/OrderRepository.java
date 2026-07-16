package com.icop.order.repository;

import com.icop.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

// the one custom query — a user's orders, newest first. spring data builds
// it from the method name; the ORDER BY is right there in the name
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
