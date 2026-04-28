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
public class JobServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public JobServiceClient(RestTemplate restTemplate, @Value("${app.services.job.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public List<JobSnapshot> getAllJobs() {
        ResponseEntity<JobSnapshot[]> response = restTemplate.exchange(
            baseUrl + "/api/v1/jobs",
            HttpMethod.GET,
            new HttpEntity<>(jsonHeaders()),
            JobSnapshot[].class
        );
        JobSnapshot[] body = response.getBody();
        return body == null ? List.of() : Arrays.asList(body);
    }

    public List<JobSnapshot> getJobsByRecruiter(Integer recruiterId) {
        ResponseEntity<JobSnapshot[]> response = restTemplate.exchange(
            baseUrl + "/api/v1/jobs/recruiter/{recruiterId}",
            HttpMethod.GET,
            new HttpEntity<>(jsonHeaders()),
            JobSnapshot[].class,
            recruiterId
        );
        JobSnapshot[] body = response.getBody();
        return body == null ? List.of() : Arrays.asList(body);
    }

    public JobSnapshot getJob(Integer jobId) {
        ResponseEntity<JobSnapshot> response = restTemplate.exchange(
            baseUrl + "/api/v1/jobs/{jobId}",
            HttpMethod.GET,
            new HttpEntity<>(jsonHeaders()),
            JobSnapshot.class,
            jobId
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
