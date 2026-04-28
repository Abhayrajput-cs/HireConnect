package com.hireconnect.notification.security;

public record AuthenticatedUser(
    String email,
    String role
) {
}
