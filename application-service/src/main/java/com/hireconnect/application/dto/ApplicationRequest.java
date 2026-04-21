package com.hireconnect.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApplicationRequest(
    @NotNull @Min(1) Integer jobId,
    @NotNull @Min(1) Integer candidateId,
    @Size(max = 4000) String coverLetter,
    @NotBlank @Size(max = 255) String resumeUrl
) {
}
