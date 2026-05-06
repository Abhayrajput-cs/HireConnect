package com.hireconnect.payment.security;

public record AuthenticatedUser(
    String email,
    String role
) {
}
