package com.hireconnect.interview.dto;

import java.time.LocalDateTime;

public record InterviewResponse(
    Integer interviewId,
    Integer applicationId,
    LocalDateTime scheduledAt,
    String mode,
    String meetLink,
    String location,
    String status,
    String notes
) {
}
