package com.hireconnect.notification.security;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class AuthValidationClient {

    private final RestTemplate restTemplate;
    private final String authServiceBaseUrl;

    public AuthValidationClient(RestTemplate restTemplate, @Value("${app.services.auth.base-url}") String authServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.authServiceBaseUrl = authServiceBaseUrl;
    }

    public AuthenticatedUser validateAccessToken(String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<TokenValidationResponse> response = restTemplate.postForEntity(
                authServiceBaseUrl + "/auth/validate",
                new HttpEntity<>(Map.of("token", token), headers),
                TokenValidationResponse.class
            );

            TokenValidationResponse body = response.getBody();
            if (body == null || !body.valid() || body.email() == null || body.role() == null) {
                return null;
            }
            return new AuthenticatedUser(body.email(), body.role());
        } catch (RestClientException ex) {
            return null;
        }
    }

    private record TokenValidationResponse(
        boolean valid,
        String email,
        String role
    ) {
    }
}
