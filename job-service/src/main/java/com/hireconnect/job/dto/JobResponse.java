package com.hireconnect.job.dto;

import java.time.LocalDate;
import java.util.List;

public record JobResponse(
    Integer jobId,
    String title,
    String category,
    String type,
    String location,
    Double salaryMin,
    Double salaryMax,
    String description,
    List<String> skills,
    Integer experienceRequired,
    Integer postedBy,
    String companyName,
    String status,
    LocalDate postedAt
) {
}
