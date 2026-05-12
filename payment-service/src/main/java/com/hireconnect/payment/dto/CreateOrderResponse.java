package com.hireconnect.payment.dto;

import java.math.BigDecimal;

import com.hireconnect.payment.domain.PaymentStatus;
import com.hireconnect.payment.domain.PlanType;

public record CreateOrderResponse(
    String orderId,
    String gatewayOrderId,
    String razorpayKeyId,
    Integer amountInPaise,
    PlanType planType,
    BigDecimal amount,
    String currency,
    PaymentStatus paymentStatus,
    Long userId,
    String customerName,
    String customerEmail,
    String customerPhone
) {
}
