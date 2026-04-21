package com.hireconnect.profile.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddressRequest(
    @NotBlank @Size(max = 80) String houseNo,
    @NotBlank @Size(max = 120) String street,
    @NotBlank @Size(max = 80) String city,
    @NotBlank @Size(max = 80) String state,
    @NotNull @Min(100000) @Max(999999) Integer pincode
) {
}
