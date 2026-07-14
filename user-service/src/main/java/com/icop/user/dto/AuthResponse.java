package com.icop.user.dto;

// what register/login hand back — the token plus enough profile info that
// the client doesn't need an immediate follow-up call
public record AuthResponse(
        String token,
        String email,
        String firstName,
        String lastName,
        String role
) {}
