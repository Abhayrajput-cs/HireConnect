package com.hireconnect.application.client;

import java.time.LocalDateTime;

public record InterviewSnapshot(
    Integer interviewId,
    Integer applicationId,
    LocalDateTime scheduledAt,
    String mode,
    String meetLink,
    String location,
    String status,
    String notes,
    LocalDateTime requestedScheduledAt,
    String requestedMeetLink,
    String requestedLocation,
    String requestedNotes
) {
}
