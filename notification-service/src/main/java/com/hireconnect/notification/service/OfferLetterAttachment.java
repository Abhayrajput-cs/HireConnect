package com.hireconnect.notification.service;

public record OfferLetterAttachment(
    String filename,
    byte[] content
) {
}
