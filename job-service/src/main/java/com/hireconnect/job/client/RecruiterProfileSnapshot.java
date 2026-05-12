package com.hireconnect.job.client;

public record RecruiterProfileSnapshot(
    Integer profileId,
    String role,
    String fullName,
    String email,
    String companyName
) {
}
