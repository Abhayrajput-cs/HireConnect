package com.hireconnect.auth.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final String defaultSuccessUrl;

    public OAuth2AuthenticationFailureHandler(@Value("${app.oauth2.default-success-url}") String defaultSuccessUrl) {
        this.defaultSuccessUrl = defaultSuccessUrl;
    }

    @Override
    public void onAuthenticationFailure(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) throws IOException, ServletException {
        var redirectCookie = CookieUtils.getCookie(request, OAuth2AuthorizationRequestFilter.REDIRECT_URI_COOKIE);
        String targetUrl = UriComponentsBuilder.fromUriString(
                redirectCookie == null ? defaultSuccessUrl : redirectCookie.getValue()
            )
            .queryParam("error", exception.getLocalizedMessage())
            .build(true)
            .toUriString();

        CookieUtils.deleteCookie(request, response, OAuth2AuthorizationRequestFilter.REDIRECT_URI_COOKIE);
        CookieUtils.deleteCookie(request, response, OAuth2AuthorizationRequestFilter.ROLE_COOKIE);
        response.sendRedirect(targetUrl);
    }
}
