package com.hireconnect.payment.service;

public record PaymentReceiptAttachment(
    String filename,
    byte[] content
) {
}
