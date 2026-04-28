package com.hireconnect.job.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final boolean messagingEnabled;
    private final String exchange;
    private final String routingKey;

    public NotificationEventPublisher(
        RabbitTemplate rabbitTemplate,
        @Value("${app.messaging.enabled:true}") boolean messagingEnabled,
        @Value("${app.messaging.exchange}") String exchange,
        @Value("${app.messaging.routing-key}") String routingKey
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.messagingEnabled = messagingEnabled;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public void publish(NotificationEvent event) {
        if (!messagingEnabled) {
            return;
        }
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
        } catch (Exception ex) {
            LOGGER.warn("Notification event publish failed: {}", ex.getMessage());
        }
    }
}
