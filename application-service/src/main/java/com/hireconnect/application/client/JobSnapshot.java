package com.hireconnect.application.client;

public record JobSnapshot(
    Integer jobId,
    Integer postedBy,
    String status,
    String title
) {
}
