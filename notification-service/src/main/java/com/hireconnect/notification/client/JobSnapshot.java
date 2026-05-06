package com.hireconnect.notification.client;

import java.time.LocalDate;

public record JobSnapshot(
    Integer jobId,
    String title,
    String category,
    String type,
    String location,
    Integer salaryMin,
    Integer salaryMax,
    String companyName,
    Integer postedBy,
    String status,
    LocalDate postedAt
) {
}
