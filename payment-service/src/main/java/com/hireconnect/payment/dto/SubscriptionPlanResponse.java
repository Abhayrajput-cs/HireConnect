package com.hireconnect.payment.dto;

import java.math.BigDecimal;
import java.util.List;

import com.hireconnect.payment.domain.PlanType;
import com.hireconnect.payment.domain.UserRole;

public record SubscriptionPlanResponse(
    Long id,
    PlanType planType,
    UserRole role,
    String displayName,
    BigDecimal amount,
    String currency,
    int durationDays,
    boolean premium,
    List<String> benefits
) {
}
