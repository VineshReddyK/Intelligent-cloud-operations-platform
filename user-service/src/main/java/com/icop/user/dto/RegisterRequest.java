package com.icop.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// validation messages are user-facing — GlobalExceptionHandler flattens them
// into the 400 response, so keep them readable
public record RegisterRequest(
        @NotBlank(message = "First name is required") String firstName,
        @NotBlank(message = "Last name is required") String lastName,
        @Email(message = "Valid email is required") @NotBlank String email,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password
) {}
