package com.hireconnect.auth.dto;

import java.time.Instant;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    Instant accessTokenExpiresAt,
    Instant refreshTokenExpiresAt,
    UserSummary user
) {
}
