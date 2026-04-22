package com.hireconnect.notification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hireconnect.notification.domain.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Integer userId);

    List<Notification> findByUserIdAndIsReadOrderByCreatedAtDesc(Integer userId, boolean isRead);

    List<Notification> findByTypeOrderByCreatedAtDesc(String type);

    int countByUserIdAndIsRead(Integer userId, boolean isRead);

    void deleteByNotificationId(Integer notificationId);
}
