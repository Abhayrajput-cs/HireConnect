package com.hireconnect.notification.client;

public record ProfileSnapshot(
    Integer profileId,
    String role,
    String fullName,
    String email
) {
}
