package com.hireconnect.auth.dto;

import java.time.LocalDateTime;

public record UserSummary(
    Integer userId,
    String fullName,
    String email,
    String role,
    String provider,
    LocalDateTime createdAt
) {
}
