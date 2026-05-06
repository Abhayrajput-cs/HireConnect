package com.hireconnect.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.razorpay")
public record RazorpayProperties(
    String baseUrl,
    String keyId,
    String keySecret,
    boolean mockMode
) {
}
