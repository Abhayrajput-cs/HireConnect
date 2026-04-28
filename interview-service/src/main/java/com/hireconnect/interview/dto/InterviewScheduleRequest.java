package com.hireconnect.interview.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InterviewScheduleRequest(
    @NotNull Integer applicationId,
    @NotNull @Future LocalDateTime scheduledAt,
    @NotBlank @Size(max = 30) String mode,
    @Size(max = 500) String meetLink,
    @Size(max = 255) String location,
    @Size(max = 2000) String notes
) {
}
