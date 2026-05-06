package com.hireconnect.payment.dto;

import java.time.Instant;

import com.hireconnect.payment.domain.PlanType;
import com.hireconnect.payment.domain.SubscriptionStatus;

public record SubscriptionStatusResponse(
    Long userId,
    boolean premiumActive,
    PlanType planType,
    SubscriptionStatus status,
    Instant expiryDate
) {
}
