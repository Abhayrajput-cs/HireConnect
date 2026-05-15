package com.hireconnect.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(
    boolean enabled,
    String from,
    String username,
    String password,
    String provider,
    String apiUrl,
    String apiKey,
    String replyTo
) {
}
