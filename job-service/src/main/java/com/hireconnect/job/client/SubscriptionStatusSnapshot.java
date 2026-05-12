package com.hireconnect.job.client;

public record SubscriptionStatusSnapshot(
    Long userId,
    boolean premiumActive,
    String planType,
    String status,
    String expiryDate
) {
}
