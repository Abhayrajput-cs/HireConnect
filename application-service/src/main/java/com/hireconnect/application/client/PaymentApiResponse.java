package com.hireconnect.application.client;

public record PaymentApiResponse<T>(
    boolean success,
    String message,
    T data,
    String timestamp
) {
}
