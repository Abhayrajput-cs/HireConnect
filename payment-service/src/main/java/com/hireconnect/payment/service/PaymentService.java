package com.hireconnect.payment.service;

import java.util.List;

import com.hireconnect.payment.domain.UserRole;
import com.hireconnect.payment.dto.CreateOrderRequest;
import com.hireconnect.payment.dto.CreateOrderResponse;
import com.hireconnect.payment.dto.PaymentTransactionResponse;
import com.hireconnect.payment.dto.SubscriptionPlanResponse;
import com.hireconnect.payment.dto.SubscriptionStatusResponse;
import com.hireconnect.payment.dto.UserSubscriptionResponse;
import com.hireconnect.payment.dto.VerifyPaymentRequest;

public interface PaymentService {

    CreateOrderResponse createOrder(CreateOrderRequest request);

    PaymentTransactionResponse verifyPayment(VerifyPaymentRequest request);

    UserSubscriptionResponse getSubscription(Long userId);

    SubscriptionStatusResponse getSubscriptionStatus(Long userId);

    List<SubscriptionPlanResponse> getPlans(UserRole role);

    UserSubscriptionResponse cancelSubscription(Long userId);
}
