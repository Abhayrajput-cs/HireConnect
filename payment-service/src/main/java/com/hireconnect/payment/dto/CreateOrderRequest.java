package com.hireconnect.payment.dto;

import com.hireconnect.payment.domain.PlanType;
import com.hireconnect.payment.domain.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
    @NotNull @Positive Long userId,
    @NotNull UserRole role,
    @NotNull PlanType planType,
    @NotBlank @Size(max = 120) String customerName,
    @NotBlank @Email @Size(max = 150) String customerEmail,
    @NotBlank @Pattern(regexp = "^[6-9]\\d{9}$", message = "customerPhone must be a valid 10-digit Indian mobile number") String customerPhone
) {
}
