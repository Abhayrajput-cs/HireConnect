package com.hireconnect.web.dto;

import java.time.LocalDate;
import java.util.List;

public record ProfileResponse(
    Integer profileId,
    String role,
    String fullName,
    String email,
    Long mobile,
    LocalDate dob,
    String gender,
    List<String> skills,
    Integer experience,
    String resumeUrl,
    String companyName,
    String companySize,
    String industry,
    String website,
    List<AddressResponse> addresses
) {
}
