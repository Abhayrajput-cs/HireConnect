package com.hireconnect.interview.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InterviewRescheduleRequest(
    @NotNull @Future LocalDateTime scheduledAt,
    @Size(max = 500) String meetLink,
    @Size(max = 255) String location,
    @Size(max = 2000) String notes
) {
}
