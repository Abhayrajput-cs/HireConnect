package com.hireconnect.payment.dto;

import java.time.Instant;

import com.hireconnect.payment.domain.PlanType;
import com.hireconnect.payment.domain.SubscriptionStatus;
import com.hireconnect.payment.domain.UserRole;

public record UserSubscriptionResponse(
    Long subscriptionId,
    Long userId,
    UserRole role,
    PlanType planType,
    String displayName,
    SubscriptionStatus status,
    boolean premium,
    Instant startDate,
    Instant expiryDate
) {
}
