package com.hireconnect.application.client;

public record CandidateProfileSnapshot(
    Integer profileId,
    String role,
    String email,
    String resumeUrl
) {
}
