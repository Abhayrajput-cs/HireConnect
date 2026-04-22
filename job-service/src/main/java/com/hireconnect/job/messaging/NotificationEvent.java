package com.hireconnect.job.messaging;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record NotificationEvent(
    String eventType,
    String notificationType,
    String message,
    List<Integer> recipientUserIds,
    List<String> recipientEmails,
    String broadcastRole,
    String emailSubject,
    String emailBody,
    Integer applicationId,
    Integer jobId,
    Integer recruiterId,
    Integer candidateId,
    String status,
    LocalDate appliedAt,
    LocalDateTime occurredAt
) {
}
