package com.hireconnect.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.messaging")
public record MessagingProperties(
    boolean enabled,
    String exchange,
    String queue,
    String routingKey
) {
}
