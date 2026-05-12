package com.hireconnect.auth.dto;

import java.time.Instant;

public record TokenValidationResponse(
    boolean valid,
    Integer userId,
    String email,
    String role,
    String provider,
    Instant expiresAt,
    String message
) {
}
