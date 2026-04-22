package com.hireconnect.web.dto;

public record PortalSession(
    Integer userId,
    String email,
    String role,
    String accessToken,
    String refreshToken
) {
}
