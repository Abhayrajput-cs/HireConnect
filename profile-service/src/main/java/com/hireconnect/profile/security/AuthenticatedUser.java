package com.hireconnect.profile.security;

public record AuthenticatedUser(
    String email,
    String role
) {
}
