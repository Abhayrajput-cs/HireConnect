package com.hireconnect.profile.dto;

public record ResumeUploadResponse(
    String fileName,
    String originalFileName,
    String contentType,
    long size,
    String resumeUrl
) {
}
