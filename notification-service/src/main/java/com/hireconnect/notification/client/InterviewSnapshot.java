package com.hireconnect.notification.client;

import java.time.LocalDateTime;

public record InterviewSnapshot(
    Integer interviewId,
    Integer applicationId,
    LocalDateTime scheduledAt,
    String status
) {
}
