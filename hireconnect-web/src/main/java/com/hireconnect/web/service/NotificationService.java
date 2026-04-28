package com.hireconnect.web.service;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import com.hireconnect.web.dto.NotificationResponse;
import com.hireconnect.web.dto.PortalSession;
import com.hireconnect.web.support.GatewayClient;

@Service
public class NotificationService {

    private final GatewayClient gatewayClient;

    public NotificationService(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    public List<NotificationResponse> getByUser(Integer userId, PortalSession session) {
        return gatewayClient.get("/api/v1/notifications/user/{userId}", session, new ParameterizedTypeReference<>() {
        }, userId);
    }

    public Integer getUnreadCount(Integer userId, PortalSession session) {
        return gatewayClient.get("/api/v1/notifications/user/{userId}/unread-count", session, Integer.class, userId);
    }

    public void markAsRead(Integer notificationId, PortalSession session) {
        gatewayClient.patch("/api/v1/notifications/{notificationId}/read", session, null, Void.class, notificationId);
    }

    public void markAllRead(Integer userId, PortalSession session) {
        gatewayClient.patch("/api/v1/notifications/user/{userId}/read-all", session, null, Void.class, userId);
    }
}
