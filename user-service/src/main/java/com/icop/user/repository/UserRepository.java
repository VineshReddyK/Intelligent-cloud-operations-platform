package com.icop.user.repository;

import com.icop.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

// spring data derives both queries from the method names — no @Query needed.
// existsByEmail beats findByEmail().isPresent() since it selects 1, not a row.
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
