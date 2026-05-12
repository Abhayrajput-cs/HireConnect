package com.hireconnect.notification.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.hireconnect.notification.dto.NotificationEvent;
import com.hireconnect.notification.service.NotificationService;

@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class NotificationEventListener {

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = "${app.messaging.queue}")
    public void handle(NotificationEvent event) {
        notificationService.sendNotification(event);
    }
}
