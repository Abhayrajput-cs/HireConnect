package com.hireconnect.payment.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hireconnect.payment.domain.UserRole;
import com.hireconnect.payment.dto.ApiResponse;
import com.hireconnect.payment.dto.CreateOrderRequest;
import com.hireconnect.payment.dto.CreateOrderResponse;
import com.hireconnect.payment.dto.PaymentTransactionResponse;
import com.hireconnect.payment.dto.SubscriptionPlanResponse;
import com.hireconnect.payment.dto.SubscriptionStatusResponse;
import com.hireconnect.payment.dto.UserSubscriptionResponse;
import com.hireconnect.payment.dto.VerifyPaymentRequest;
import com.hireconnect.payment.service.PaymentService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentResource {

    private final PaymentService paymentService;

    public PaymentResource(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<CreateOrderResponse>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Payment order created", paymentService.createOrder(request)));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<PaymentTransactionResponse>> verify(@Valid @RequestBody VerifyPaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Payment verification processed", paymentService.verifyPayment(request)));
    }

    @GetMapping("/subscription/{userId}")
    public ResponseEntity<ApiResponse<UserSubscriptionResponse>> getSubscription(@PathVariable @Positive Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("Subscription fetched", paymentService.getSubscription(userId)));
    }

    @GetMapping("/subscription/status/{userId}")
    public ResponseEntity<ApiResponse<SubscriptionStatusResponse>> getSubscriptionStatus(@PathVariable @Positive Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("Subscription status fetched", paymentService.getSubscriptionStatus(userId)));
    }

    @GetMapping("/plans/{role}")
    public ResponseEntity<ApiResponse<List<SubscriptionPlanResponse>>> getPlans(@PathVariable UserRole role) {
        return ResponseEntity.ok(ApiResponse.ok("Subscription plans fetched", paymentService.getPlans(role)));
    }

    @PostMapping("/cancel/{userId}")
    public ResponseEntity<ApiResponse<UserSubscriptionResponse>> cancel(@PathVariable @Positive Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("Subscription cancelled", paymentService.cancelSubscription(userId)));
    }
}
