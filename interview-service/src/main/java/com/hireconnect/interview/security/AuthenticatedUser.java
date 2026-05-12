package com.hireconnect.interview.security;

public record AuthenticatedUser(
    String email,
    String role
) {
}
