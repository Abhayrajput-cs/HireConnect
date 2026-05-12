package com.hireconnect.interview.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.hireconnect.interview.exception.ApiException;

@Component
public class ApplicationServiceClient implements ApplicationCatalogClient {

    private final RestClient restClient;

    public ApplicationServiceClient(@Value("${app.services.application.base-url}") String applicationServiceBaseUrl) {
        this.restClient = RestClient.builder().baseUrl(applicationServiceBaseUrl).build();
    }

    @Override
    public ApplicationSnapshot getApplication(Integer applicationId) {
        try {
            RestClient.RequestHeadersSpec<?> request = restClient.get()
                .uri("/api/v1/applications/{applicationId}", applicationId);
            String authorizationHeader = currentAuthorizationHeader();
            if (StringUtils.hasText(authorizationHeader)) {
                request = request.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
            }
            return request.retrieve().body(ApplicationSnapshot.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Application not found with id: " + applicationId);
        } catch (HttpClientErrorException.Unauthorized ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication is required to validate the application");
        } catch (ResourceAccessException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Application service is unavailable");
        } catch (RestClientResponseException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Application service returned an unexpected response");
        }
    }

    @Override
    public void markInterviewScheduled(Integer applicationId) {
        try {
            RestClient.RequestBodySpec request = restClient.patch()
                .uri("/api/v1/applications/{applicationId}/status", applicationId);
            String authorizationHeader = currentAuthorizationHeader();
            if (StringUtils.hasText(authorizationHeader)) {
                request = request.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
            }
            request
                .body(new InterviewStatusUpdateRequest("INTERVIEW_SCHEDULED"))
                .retrieve()
                .toBodilessEntity();
        } catch (HttpClientErrorException.BadRequest ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Application cannot move to INTERVIEW_SCHEDULED");
        } catch (HttpClientErrorException.Unauthorized ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication is required to update the application status");
        } catch (ResourceAccessException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Application service is unavailable");
        } catch (RestClientResponseException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Application service returned an unexpected response");
        }
    }

    private String currentAuthorizationHeader() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
    }

    private record InterviewStatusUpdateRequest(String status) {
    }
}
