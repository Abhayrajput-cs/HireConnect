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

import com.hireconnect.notification.exception.ApiException;

import org.springframework.http.HttpStatus;

@Component
public class ProfileServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ProfileServiceClient(RestTemplate restTemplate, @Value("${app.services.profile.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public ProfileSnapshot getProfileByEmail(String email) {
        ResponseEntity<ProfileSnapshot> response = restTemplate.exchange(
            baseUrl + "/api/v1/profiles/email/{email}",
            HttpMethod.GET,
            new HttpEntity<>(jsonHeaders()),
            ProfileSnapshot.class,
            email
        );
        return response.getBody();
    }

    public ProfileSnapshot getProfileById(Integer profileId) {
        ResponseEntity<ProfileSnapshot> response = restTemplate.exchange(
            baseUrl + "/api/v1/profiles/{profileId}",
            HttpMethod.GET,
            new HttpEntity<>(jsonHeaders()),
            ProfileSnapshot.class,
            profileId
        );
        return response.getBody();
    }

    public List<ProfileSnapshot> getProfilesByRole(String role) {
        ResponseEntity<ProfileSnapshot[]> response = restTemplate.exchange(
            baseUrl + "/api/v1/profiles/role/{role}",
            HttpMethod.GET,
            new HttpEntity<>(jsonHeaders()),
            ProfileSnapshot[].class,
            role
        );
        ProfileSnapshot[] body = response.getBody();
        return body == null ? List.of() : Arrays.asList(body);
    }

    public ProfileSnapshot getProfileByEmailForAuthUser(String email) {
        ProfileSnapshot profile = getProfileByEmail(email);
        if (profile == null || profile.profileId() == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Profile service did not return the authenticated user profile");
        }
        return profile;
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
