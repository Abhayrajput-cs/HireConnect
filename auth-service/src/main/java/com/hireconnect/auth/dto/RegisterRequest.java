package com.hireconnect.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank
    @Size(min = 2, max = 120)
    @Pattern(
        regexp = "^[A-Za-z][A-Za-z .'-]*$",
        message = "full name may contain letters, spaces, apostrophes, hyphens and dots"
    )
    String fullName,
    @NotBlank @Email String email,
    @NotBlank
    @Size(min = 8, max = 72)
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
        message = "password must contain upper-case, lower-case, and numeric characters"
    )
    String password,
    @NotBlank String role
) {
}
