package com.hireconnect.interview.client;

public record ApplicationSnapshot(
    Integer applicationId,
    Integer jobId,
    Integer candidateId,
    String status
) {
}
