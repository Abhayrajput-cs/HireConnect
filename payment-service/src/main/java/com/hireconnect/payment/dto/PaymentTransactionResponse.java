package com.hireconnect.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.hireconnect.payment.domain.PaymentStatus;
import com.hireconnect.payment.domain.PlanType;
import com.hireconnect.payment.domain.UserRole;

public record PaymentTransactionResponse(
    Long id,
    String orderId,
    String transactionId,
    Long userId,
    UserRole role,
    PlanType planType,
    BigDecimal amount,
    String currency,
    PaymentStatus paymentStatus,
    Instant startDate,
    Instant expiryDate
) {
}
