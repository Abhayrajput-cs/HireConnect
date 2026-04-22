package com.hireconnect.notification.client;

import java.time.LocalDate;

public record ApplicationSnapshot(
    Integer applicationId,
    Integer jobId,
    Integer candidateId,
    LocalDate appliedAt,
    String status
) {
}
