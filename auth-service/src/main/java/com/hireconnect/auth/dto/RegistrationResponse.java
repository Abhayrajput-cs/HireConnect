package com.hireconnect.auth.dto;

public record RegistrationResponse(
    String email,
    String role,
    boolean verificationRequired,
    String message
) {
}
