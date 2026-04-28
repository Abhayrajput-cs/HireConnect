package com.hireconnect.notification.client;

import java.time.LocalDate;

public record JobSnapshot(
    Integer jobId,
    String title,
    String category,
    Integer postedBy,
    String status,
    LocalDate postedAt
) {
}
