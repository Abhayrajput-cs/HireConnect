package com.hireconnect.interview.client;

public record JobSnapshot(
    Integer jobId,
    Integer postedBy,
    String status,
    String title
) {
}
