package com.hireconnect.job.security;

public record AuthenticatedUser(
    String email,
    String role
) {
}
