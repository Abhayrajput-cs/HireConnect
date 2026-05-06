package com.hireconnect.application.client;

import java.util.Arrays;
import java.util.List;

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

import com.hireconnect.application.exception.ApiException;

@Component
public class InterviewServiceClient implements InterviewCatalogClient {

    private final RestClient restClient;

    public InterviewServiceClient(@Value("${app.services.interview.base-url}") String interviewServiceBaseUrl) {
        this.restClient = RestClient.builder().baseUrl(interviewServiceBaseUrl).build();
    }

    @Override
    public List<InterviewSnapshot> getByApplication(Integer applicationId) {
        try {
            RestClient.RequestHeadersSpec<?> request = restClient.get()
                .uri("/api/v1/interviews/application/{applicationId}", applicationId);
            String authorizationHeader = currentAuthorizationHeader();
            if (StringUtils.hasText(authorizationHeader)) {
                request = request.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
            }
            InterviewSnapshot[] interviews = request.retrieve().body(InterviewSnapshot[].class);
            return interviews == null ? List.of() : Arrays.asList(interviews);
        } catch (HttpClientErrorException.NotFound ex) {
            return List.of();
        } catch (HttpClientErrorException.Unauthorized ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication is required to validate interview progress");
        } catch (ResourceAccessException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Interview service is unavailable for application status validation");
        } catch (RestClientResponseException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Interview service returned an unexpected response");
        }
    }

    private String currentAuthorizationHeader() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
    }
}
