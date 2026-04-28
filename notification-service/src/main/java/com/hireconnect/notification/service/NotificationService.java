package com.hireconnect.notification.service;

import java.util.List;

import com.hireconnect.notification.dto.NotificationEvent;
import com.hireconnect.notification.dto.NotificationResponse;

public interface NotificationService {

    void sendNotification(NotificationEvent event);

    void markAsRead(Integer notificationId);

    void markAllRead(Integer userId);

    List<NotificationResponse> getByUser(Integer userId, Boolean isRead);

    void deleteNotification(Integer notificationId);

    void sendEmailAlert(String toEmail, String subject, String body);

    int getUnreadCount(Integer userId);
}
