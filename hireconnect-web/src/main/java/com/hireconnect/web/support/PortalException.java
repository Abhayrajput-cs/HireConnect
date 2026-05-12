package com.hireconnect.web.support;

import org.springframework.http.HttpStatusCode;

public class PortalException extends RuntimeException {

    private final HttpStatusCode statusCode;

    public PortalException(HttpStatusCode statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }
}
