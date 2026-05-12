package com.hireconnect.job.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record JobRequest(
    @NotBlank @Size(max = 120) String title,
    @NotBlank @Size(max = 80) String category,
    @NotBlank @Size(max = 50) String type,
    @NotBlank @Size(max = 120) String location,
    @NotNull @PositiveOrZero Double salaryMin,
    @NotNull @PositiveOrZero Double salaryMax,
    @NotBlank @Size(max = 4000) String description,
    @NotEmpty List<@NotBlank @Size(max = 80) String> skills,
    @NotNull @PositiveOrZero Integer experienceRequired,
    @NotNull @Positive Integer postedBy,
    @Size(max = 30) String status,
    LocalDate postedAt
) {
}
