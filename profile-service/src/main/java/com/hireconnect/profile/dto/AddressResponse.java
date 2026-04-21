package com.hireconnect.profile.dto;

public record AddressResponse(
    Integer addressId,
    String houseNo,
    String street,
    String city,
    String state,
    Integer pincode
) {
}
