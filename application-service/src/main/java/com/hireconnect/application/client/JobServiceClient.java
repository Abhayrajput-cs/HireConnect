package com.hireconnect.application.client;

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
public class JobServiceClient implements JobCatalogClient {

    private final RestClient restClient;

    public JobServiceClient(@Value("${app.services.job.base-url}") String jobServiceBaseUrl) {
        this.restClient = RestClient.builder().baseUrl(jobServiceBaseUrl).build();
    }

    @Override
    public JobSnapshot getJob(Integer jobId) {
        try {
            RestClient.RequestHeadersSpec<?> request = restClient.get()
                .uri("/api/v1/jobs/{jobId}", jobId);
            String authorizationHeader = currentAuthorizationHeader();
            if (StringUtils.hasText(authorizationHeader)) {
                request = request.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
            }
            return request.retrieve().body(JobSnapshot.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Job not found with id: " + jobId);
        } catch (HttpClientErrorException.Unauthorized ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication is required to validate the job");
        } catch (ResourceAccessException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Job service is unavailable for application submission");
        } catch (RestClientResponseException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Job service returned an unexpected response");
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
