package com.hireconnect.web.support;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireconnect.web.dto.PortalSession;

@Service
public class GatewayClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public GatewayClient(
        RestTemplate restTemplate,
        ObjectMapper objectMapper,
        @Value("${app.gateway.base-url}") String baseUrl
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
    }

    public <T> T get(String path, PortalSession session, Class<T> responseType, Object... uriVariables) {
        return exchange(path, HttpMethod.GET, session, null, responseType, uriVariables);
    }

    public <T> T post(String path, PortalSession session, Object body, Class<T> responseType, Object... uriVariables) {
        return exchange(path, HttpMethod.POST, session, body, responseType, uriVariables);
    }

    public <T> T put(String path, PortalSession session, Object body, Class<T> responseType, Object... uriVariables) {
        return exchange(path, HttpMethod.PUT, session, body, responseType, uriVariables);
    }

    public <T> T patch(String path, PortalSession session, Object body, Class<T> responseType, Object... uriVariables) {
        return exchange(path, HttpMethod.PATCH, session, body, responseType, uriVariables);
    }

    public void delete(String path, PortalSession session, Object... uriVariables) {
        exchange(path, HttpMethod.DELETE, session, null, Void.class, uriVariables);
    }

    public <T> T get(String path, PortalSession session, ParameterizedTypeReference<T> responseType, Object... uriVariables) {
        return exchange(path, HttpMethod.GET, session, null, responseType, uriVariables);
    }

    private <T> T exchange(
        String path,
        HttpMethod method,
        PortalSession session,
        Object body,
        Class<T> responseType,
        Object... uriVariables
    ) {
        try {
            ResponseEntity<T> response = restTemplate.exchange(
                baseUrl + path,
                method,
                new HttpEntity<>(body, headers(session)),
                responseType,
                uriVariables
            );
            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            throw new PortalException(ex.getStatusCode(), extractMessage(ex));
        }
    }

    private <T> T exchange(
        String path,
        HttpMethod method,
        PortalSession session,
        Object body,
        ParameterizedTypeReference<T> responseType,
        Object... uriVariables
    ) {
        try {
            ResponseEntity<T> response = restTemplate.exchange(
                baseUrl + path,
                method,
                new HttpEntity<>(body, headers(session)),
                responseType,
                uriVariables
            );
            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            throw new PortalException(ex.getStatusCode(), extractMessage(ex));
        }
    }

    private HttpHeaders headers(PortalSession session) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        if (session != null && StringUtils.hasText(session.accessToken())) {
            headers.setBearerAuth(session.accessToken());
        }
        if (headers.getContentType() == null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return headers;
    }

    private String extractMessage(HttpStatusCodeException ex) {
        String body = ex.getResponseBodyAsString(StandardCharsets.UTF_8);
        if (!StringUtils.hasText(body)) {
            return ex.getStatusText();
        }
        try {
            JsonNode json = objectMapper.readTree(body);
            if (json.hasNonNull("message")) {
                return json.get("message").asText();
            }
            if (json.hasNonNull("error")) {
                return json.get("error").asText();
            }
        } catch (Exception ignored) {
        }
        return body;
    }
}
