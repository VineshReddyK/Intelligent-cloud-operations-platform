package com.icop.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// no @Size on password here — length rules only matter at registration,
// checking them at login would just leak our password policy
public record LoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password
) {}
