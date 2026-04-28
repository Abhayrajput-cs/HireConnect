package com.hireconnect.web.dto;

import java.time.LocalDate;

public record ApplicationResponse(
    Integer applicationId,
    Integer jobId,
    Integer candidateId,
    LocalDate appliedAt,
    String status,
    String coverLetter,
    String resumeUrl
) {
}
