package com.hireconnect.payment.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.hireconnect.payment.config.RazorpayProperties;
import com.hireconnect.payment.domain.PaymentStatus;
import com.hireconnect.payment.exception.ApiException;

@Component
public class RazorpayClient {

    private final RazorpayProperties properties;
    private final RestClient restClient;

    public RazorpayClient(RazorpayProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
            .baseUrl(properties.baseUrl())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    public RazorpayOrderResponse createOrder(RazorpayOrderRequest request) {
        if (properties.mockMode()) {
            return new RazorpayOrderResponse("order_mock_" + UUID.randomUUID(), toPaise(request.amount()), properties.keyId());
        }

        assertCredentialsConfigured();
        try {
            Map<?, ?> response = restClient.post()
                .uri("/v1/orders")
                .header(HttpHeaders.AUTHORIZATION, basicAuth())
                .body(Map.of(
                    "amount", toPaise(request.amount()),
                    "currency", request.currency(),
                    "receipt", request.orderId(),
                    "notes", Map.of(
                        "hireconnect_order_id", request.orderId(),
                        "user_id", String.valueOf(request.userId())
                    )
                ))
                .retrieve()
                .body(Map.class);

            if (response == null || response.get("id") == null) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Razorpay did not return an order id");
            }

            return new RazorpayOrderResponse(
                String.valueOf(response.get("id")),
                toPaise(request.amount()),
                properties.keyId()
            );
        } catch (RestClientException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Razorpay order creation failed");
        }
    }

    public RazorpayVerificationResult verifyPayment(RazorpayVerificationRequest request) {
        if (properties.mockMode()) {
            return new RazorpayVerificationResult(
                PaymentStatus.SUCCESS,
                StringUtils.hasText(request.razorpayPaymentId()) ? request.razorpayPaymentId() : "pay_mock_" + UUID.randomUUID(),
                null
            );
        }

        assertCredentialsConfigured();
        if (!StringUtils.hasText(request.razorpayOrderId())
            || !StringUtils.hasText(request.razorpayPaymentId())
            || !StringUtils.hasText(request.razorpaySignature())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Razorpay payment id, order id and signature are required");
        }

        String payload = request.razorpayOrderId() + "|" + request.razorpayPaymentId();
        String expected = hmacSha256(payload, properties.keySecret());
        if (!expected.equals(request.razorpaySignature())) {
            return new RazorpayVerificationResult(PaymentStatus.FAILED, request.razorpayPaymentId(), "Invalid Razorpay signature");
        }

        return new RazorpayVerificationResult(PaymentStatus.SUCCESS, request.razorpayPaymentId(), null);
    }

    private int toPaise(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    private String basicAuth() {
        return "Basic " + Base64.getEncoder()
            .encodeToString((properties.keyId() + ":" + properties.keySecret()).getBytes(StandardCharsets.UTF_8));
    }

    private String hmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Razorpay verification could not be completed right now. Please try again.");
        }
    }

    private void assertCredentialsConfigured() {
        if (!StringUtils.hasText(properties.keyId()) || !StringUtils.hasText(properties.keySecret())) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Razorpay payment gateway is not configured. Please contact support.");
        }
    }

    public record RazorpayOrderRequest(
        String orderId,
        Long userId,
        BigDecimal amount,
        String currency
    ) {
    }

    public record RazorpayOrderResponse(
        String razorpayOrderId,
        int amountInPaise,
        String keyId
    ) {
    }

    public record RazorpayVerificationRequest(
        String razorpayOrderId,
        String razorpayPaymentId,
        String razorpaySignature
    ) {
    }

    public record RazorpayVerificationResult(
        PaymentStatus paymentStatus,
        String transactionId,
        String failureReason
    ) {
    }
}
