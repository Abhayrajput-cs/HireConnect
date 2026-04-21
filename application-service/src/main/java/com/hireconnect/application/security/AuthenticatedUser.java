package com.hireconnect.application.security;

public record AuthenticatedUser(
    String email,
    String role
) {
}
