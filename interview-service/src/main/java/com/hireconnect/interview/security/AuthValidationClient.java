package com.hireconnect.interview.security;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AuthValidationClient {

    private final RestClient restClient;

    public AuthValidationClient(@Value("${app.services.auth.base-url}") String authServiceBaseUrl) {
        this.restClient = RestClient.builder().baseUrl(authServiceBaseUrl).build();
    }

    public AuthenticatedUser validateAccessToken(String token) {
        try {
            TokenValidationResponse response = restClient.post()
                .uri("/auth/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Map.of("token", token))
                .retrieve()
                .body(TokenValidationResponse.class);

            if (response == null || !response.valid() || response.email() == null || response.role() == null) {
                return null;
            }

            return new AuthenticatedUser(response.email(), response.role());
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
