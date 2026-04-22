package com.hireconnect.notification.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
    Integer notificationId,
    Integer userId,
    String type,
    String message,
    boolean isRead,
    LocalDateTime createdAt
) {
}
