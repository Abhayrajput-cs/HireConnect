package com.hireconnect.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyPaymentRequest(
    @NotBlank @Size(max = 80) String orderId,
    @Size(max = 120) String transactionId,
    @Size(max = 120) String razorpayOrderId,
    @Size(max = 120) String razorpayPaymentId,
    @Size(max = 255) String razorpaySignature
) {
}
