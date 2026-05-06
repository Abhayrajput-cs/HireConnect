package com.hireconnect.payment.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.hireconnect.payment.domain.PaymentStatus;
import com.hireconnect.payment.domain.PaymentTransaction;
import com.hireconnect.payment.domain.PlanType;
import com.hireconnect.payment.domain.SubscriptionPlan;
import com.hireconnect.payment.domain.SubscriptionStatus;
import com.hireconnect.payment.domain.UserRole;
import com.hireconnect.payment.domain.UserSubscription;
import com.hireconnect.payment.config.RazorpayProperties;
import com.hireconnect.payment.dto.CreateOrderRequest;
import com.hireconnect.payment.dto.CreateOrderResponse;
import com.hireconnect.payment.dto.PaymentTransactionResponse;
import com.hireconnect.payment.dto.SubscriptionPlanResponse;
import com.hireconnect.payment.dto.SubscriptionStatusResponse;
import com.hireconnect.payment.dto.UserSubscriptionResponse;
import com.hireconnect.payment.dto.VerifyPaymentRequest;
import com.hireconnect.payment.exception.ApiException;
import com.hireconnect.payment.repository.PaymentTransactionRepository;
import com.hireconnect.payment.repository.SubscriptionPlanRepository;
import com.hireconnect.payment.repository.UserSubscriptionRepository;
import com.hireconnect.payment.service.RazorpayClient.RazorpayOrderRequest;
import com.hireconnect.payment.service.RazorpayClient.RazorpayVerificationRequest;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final SubscriptionPlanRepository plans;
    private final UserSubscriptionRepository subscriptions;
    private final PaymentTransactionRepository transactions;
    private final RazorpayClient razorpayClient;
    private final RazorpayProperties razorpayProperties;
    private final PaymentReceiptMailService receiptMailService;

    public PaymentServiceImpl(
        SubscriptionPlanRepository plans,
        UserSubscriptionRepository subscriptions,
        PaymentTransactionRepository transactions,
        RazorpayClient razorpayClient,
        RazorpayProperties razorpayProperties,
        PaymentReceiptMailService receiptMailService
    ) {
        this.plans = plans;
        this.subscriptions = subscriptions;
        this.transactions = transactions;
        this.razorpayClient = razorpayClient;
        this.razorpayProperties = razorpayProperties;
        this.receiptMailService = receiptMailService;
    }

    @Override
    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        SubscriptionPlan plan = findPlan(request.planType());
        validateRoleMatchesPlan(request.role(), plan);

        String orderId = "HC_" + request.userId() + "_" + System.currentTimeMillis();
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrderId(orderId);
        transaction.setUserId(request.userId());
        transaction.setCustomerName(request.customerName());
        transaction.setCustomerEmail(request.customerEmail());
        transaction.setCustomerPhone(request.customerPhone());
        transaction.setRole(request.role());
        transaction.setPlanType(plan.getPlanType());
        transaction.setAmount(plan.getAmount());
        transaction.setCurrency(plan.getCurrency());
        transaction.setPaymentStatus(plan.isPremium() ? PaymentStatus.CREATED : PaymentStatus.SUCCESS);

        if (!plan.isPremium()) {
            activateSubscription(request.userId(), request.role(), plan, transaction);
            PaymentTransaction saved = transactions.save(transaction);
            sendReceiptWithoutBlockingPayment(saved, plan);
            return toCreateOrderResponse(saved);
        }

        var razorpayOrder = razorpayClient.createOrder(new RazorpayOrderRequest(
            orderId,
            request.userId(),
            plan.getAmount(),
            plan.getCurrency()
        ));
        transaction.setGatewayOrderId(razorpayOrder.razorpayOrderId());
        transaction.setAmountInPaise(razorpayOrder.amountInPaise());
        PaymentTransaction saved = transactions.save(transaction);
        return toCreateOrderResponse(saved);
    }

    @Override
    @Transactional
    public PaymentTransactionResponse verifyPayment(VerifyPaymentRequest request) {
        PaymentTransaction transaction = transactions.findByOrderId(request.orderId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment order not found"));

        if (transaction.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return toTransactionResponse(transaction);
        }

        if (!StringUtils.hasText(transaction.getGatewayOrderId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This payment order was not created with Razorpay. Please choose the premium plan again.");
        }

        if (!StringUtils.hasText(request.razorpayPaymentId()) && !StringUtils.hasText(request.transactionId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Razorpay payment was not completed. Please finish the payment before verification.");
        }

        if (!StringUtils.hasText(request.razorpaySignature())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Razorpay did not return a verification signature. Please try the payment again.");
        }

        var verification = razorpayClient.verifyPayment(new RazorpayVerificationRequest(
            transaction.getGatewayOrderId(),
            request.razorpayPaymentId() != null ? request.razorpayPaymentId() : request.transactionId(),
            request.razorpaySignature()
        ));
        transaction.setPaymentStatus(verification.paymentStatus());
        transaction.setTransactionId(verification.transactionId());
        transaction.setFailureReason(verification.failureReason());
        transaction.setUpdatedAt(Instant.now());

        try {
            if (verification.paymentStatus() == PaymentStatus.SUCCESS) {
                SubscriptionPlan plan = findPlan(transaction.getPlanType());
                activateSubscription(transaction.getUserId(), transaction.getRole(), plan, transaction);
            }

            PaymentTransaction saved = transactions.save(transaction);
            if (saved.getPaymentStatus() == PaymentStatus.SUCCESS) {
                sendReceiptWithoutBlockingPayment(saved, findPlan(saved.getPlanType()));
            }
            return toTransactionResponse(saved);
        } catch (DataAccessException ex) {
            log.error("Payment verified by Razorpay, but subscription activation failed for order {}", transaction.getOrderId(), ex);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Payment was verified, but subscription activation could not be completed. Please retry verification or contact support with this order id.");
        }
    }

    private void sendReceiptWithoutBlockingPayment(PaymentTransaction transaction, SubscriptionPlan plan) {
        try {
            receiptMailService.sendSubscriptionReceipt(transaction, plan);
        } catch (RuntimeException ex) {
            log.warn("Subscription activated, but receipt email failed for order {}", transaction.getOrderId(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserSubscriptionResponse getSubscription(Long userId) {
        return subscriptions.findFirstByUserIdAndStatusOrderByExpiryDateDesc(userId, SubscriptionStatus.ACTIVE)
            .map(this::toSubscriptionResponse)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No active subscription found for this user"));
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionStatusResponse getSubscriptionStatus(Long userId) {
        Instant now = Instant.now();
        return subscriptions.findFirstByUserIdAndStatusOrderByExpiryDateDesc(userId, SubscriptionStatus.ACTIVE)
            .map(subscription -> {
                boolean active = subscription.getExpiryDate().isAfter(now);
                return new SubscriptionStatusResponse(
                    userId,
                    active && subscription.getPlan().isPremium(),
                    subscription.getPlan().getPlanType(),
                    active ? subscription.getStatus() : SubscriptionStatus.EXPIRED,
                    subscription.getExpiryDate()
                );
            })
            .orElse(new SubscriptionStatusResponse(userId, false, null, SubscriptionStatus.EXPIRED, null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> getPlans(UserRole role) {
        return plans.findByRoleAndActiveTrueOrderByAmountAsc(role).stream()
            .map(this::toPlanResponse)
            .toList();
    }

    @Override
    @Transactional
    public UserSubscriptionResponse cancelSubscription(Long userId) {
        UserSubscription subscription = subscriptions.findFirstByUserIdAndStatusOrderByExpiryDateDesc(userId, SubscriptionStatus.ACTIVE)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No active subscription found to cancel"));
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setUpdatedAt(Instant.now());
        return toSubscriptionResponse(subscriptions.save(subscription));
    }

    private void activateSubscription(Long userId, UserRole role, SubscriptionPlan plan, PaymentTransaction transaction) {
        Instant now = Instant.now();
        subscriptions.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE).forEach(existing -> {
            existing.setStatus(SubscriptionStatus.CANCELLED);
            existing.setUpdatedAt(now);
            subscriptions.save(existing);
        });

        UserSubscription subscription = new UserSubscription();
        subscription.setUserId(userId);
        subscription.setRole(role);
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(now);
        subscription.setExpiryDate(now.plus(plan.getDurationDays(), ChronoUnit.DAYS));
        subscriptions.save(subscription);

        transaction.setStartDate(subscription.getStartDate());
        transaction.setExpiryDate(subscription.getExpiryDate());
        if (transaction.getTransactionId() == null) {
            transaction.setTransactionId("free_" + UUID.randomUUID());
        }
    }

    private SubscriptionPlan findPlan(PlanType planType) {
        return plans.findByPlanTypeAndActiveTrue(planType)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subscription plan not found or inactive"));
    }

    private void validateRoleMatchesPlan(UserRole role, SubscriptionPlan plan) {
        if (plan.getRole() != role) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Selected plan does not match the user's role");
        }
    }

    private CreateOrderResponse toCreateOrderResponse(PaymentTransaction transaction) {
        return new CreateOrderResponse(
            transaction.getOrderId(),
            transaction.getGatewayOrderId(),
            razorpayProperties.keyId(),
            transaction.getAmountInPaise(),
            transaction.getPlanType(),
            transaction.getAmount(),
            transaction.getCurrency(),
            transaction.getPaymentStatus()
        );
    }


    private PaymentTransactionResponse toTransactionResponse(PaymentTransaction transaction) {
        return new PaymentTransactionResponse(
            transaction.getId(),
            transaction.getOrderId(),
            transaction.getTransactionId(),
            transaction.getUserId(),
            transaction.getRole(),
            transaction.getPlanType(),
            transaction.getAmount(),
            transaction.getCurrency(),
            transaction.getPaymentStatus(),
            transaction.getStartDate(),
            transaction.getExpiryDate()
        );
    }

    private SubscriptionPlanResponse toPlanResponse(SubscriptionPlan plan) {
        return new SubscriptionPlanResponse(
            plan.getId(),
            plan.getPlanType(),
            plan.getRole(),
            plan.getDisplayName(),
            plan.getAmount(),
            plan.getCurrency(),
            plan.getDurationDays(),
            plan.isPremium(),
            List.copyOf(plan.getBenefits())
        );
    }

    private UserSubscriptionResponse toSubscriptionResponse(UserSubscription subscription) {
        return new UserSubscriptionResponse(
            subscription.getId(),
            subscription.getUserId(),
            subscription.getRole(),
            subscription.getPlan().getPlanType(),
            subscription.getPlan().getDisplayName(),
            subscription.getStatus(),
            subscription.getPlan().isPremium(),
            subscription.getStartDate(),
            subscription.getExpiryDate()
        );
    }
}
