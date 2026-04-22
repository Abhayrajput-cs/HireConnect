package com.hireconnect.notification.client;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class ApplicationServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ApplicationServiceClient(RestTemplate restTemplate, @Value("${app.services.application.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public List<ApplicationSnapshot> getApplicationsByJob(Integer jobId) {
        ResponseEntity<ApplicationSnapshot[]> response = restTemplate.exchange(
            baseUrl + "/api/v1/applications/job/{jobId}",
            HttpMethod.GET,
            new HttpEntity<>(jsonHeaders()),
            ApplicationSnapshot[].class,
            jobId
        );
        ApplicationSnapshot[] body = response.getBody();
        return body == null ? List.of() : Arrays.asList(body);
    }

    public int getApplicationCountByJob(Integer jobId) {
        ResponseEntity<Integer> response = restTemplate.exchange(
            baseUrl + "/api/v1/applications/job/{jobId}/count",
            HttpMethod.GET,
            new HttpEntity<>(jsonHeaders()),
            Integer.class,
            jobId
        );
        Integer body = response.getBody();
        return body == null ? 0 : body;
    }

    public ApplicationSnapshot getApplication(Integer applicationId) {
        ResponseEntity<ApplicationSnapshot> response = restTemplate.exchange(
            baseUrl + "/api/v1/applications/{applicationId}",
            HttpMethod.GET,
            new HttpEntity<>(jsonHeaders()),
            ApplicationSnapshot.class,
            applicationId
        );
        return response.getBody();
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        String authorizationHeader = currentAuthorizationHeader();
        if (StringUtils.hasText(authorizationHeader)) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        return headers;
    }

    private String currentAuthorizationHeader() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
    }
}
