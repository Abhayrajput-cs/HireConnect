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
public class ProfileServiceClient implements ProfileDirectoryClient {

    private final RestClient restClient;

    public ProfileServiceClient(@Value("${app.services.profile.base-url}") String profileServiceBaseUrl) {
        this.restClient = RestClient.builder().baseUrl(profileServiceBaseUrl).build();
    }

    @Override
    public ProfileSnapshot getProfileByEmail(String email) {
        try {
            RestClient.RequestHeadersSpec<?> request = restClient.get().uri("/api/v1/profiles/email/{email}", email);
            String authorizationHeader = currentAuthorizationHeader();
            if (StringUtils.hasText(authorizationHeader)) {
                request = request.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
            }
            return request.retrieve().body(ProfileSnapshot.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Profile not found for authenticated user");
        } catch (HttpClientErrorException.Unauthorized ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication is required to validate the profile");
        } catch (ResourceAccessException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Profile service is unavailable");
        } catch (RestClientResponseException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Profile service returned an unexpected response");
        }
    }

    @Override
    public ProfileSnapshot getProfileById(Integer profileId) {
        try {
            RestClient.RequestHeadersSpec<?> request = restClient.get().uri("/api/v1/profiles/{profileId}", profileId);
            String authorizationHeader = currentAuthorizationHeader();
            if (StringUtils.hasText(authorizationHeader)) {
                request = request.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
            }
            return request.retrieve().body(ProfileSnapshot.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Profile not found with id: " + profileId);
        } catch (HttpClientErrorException.Unauthorized ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication is required to validate the profile");
        } catch (ResourceAccessException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Profile service is unavailable");
        } catch (RestClientResponseException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Profile service returned an unexpected response");
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
