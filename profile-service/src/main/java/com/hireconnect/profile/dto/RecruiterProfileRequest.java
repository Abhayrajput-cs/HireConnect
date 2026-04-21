package com.hireconnect.profile.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RecruiterProfileRequest(
    @NotBlank @Size(max = 120) String fullName,
    @NotBlank @Email @Size(max = 150) String email,
    @Positive Long mobile,
    LocalDate dob,
    @Size(max = 20) String gender,
    @NotBlank @Size(max = 120) String companyName,
    @Size(max = 40) String companySize,
    @NotBlank @Size(max = 80) String industry,
    @Size(max = 255) String website,
    @Valid List<AddressRequest> addresses
) {
}
