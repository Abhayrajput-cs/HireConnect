package com.hireconnect.application.client;

public record SubscriptionStatusSnapshot(
    Long userId,
    boolean premiumActive,
    String planType,
    String status,
    String expiryDate
) {
}
