package com.hireconnect.profile.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CandidateProfileRequest(
    @NotBlank @Size(max = 120) String fullName,
    @NotBlank @Email @Size(max = 150) String email,
    @NotNull @Min(6000000000L) @Max(9999999999L) Long mobile,
    LocalDate dob,
    @Size(max = 20) String gender,
    @NotEmpty List<@NotBlank @Size(max = 80) String> skills,
    @NotNull @Min(0) Integer experience,
    @NotBlank @Size(max = 255) String resumeUrl,
    @Valid List<AddressRequest> addresses
) {
}
