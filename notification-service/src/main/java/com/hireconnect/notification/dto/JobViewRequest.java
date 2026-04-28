package com.hireconnect.notification.dto;

import java.time.LocalDateTime;

public record JobViewRequest(
    Integer viewerId,
    LocalDateTime occurredAt
) {
}
