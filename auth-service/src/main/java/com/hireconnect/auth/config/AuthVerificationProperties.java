package com.hireconnect.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.email-verification")
public record AuthVerificationProperties(
    boolean enabled,
    long otpTtlMinutes
) {
}
