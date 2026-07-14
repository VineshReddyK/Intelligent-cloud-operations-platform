package com.icop.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

// the public view of a user — same shape as the entity minus the password
// hash, which must never cross the API boundary
public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String role,
        LocalDateTime createdAt
) {}
