package com.hireconnect.auth.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2AuthorizationRequestFilter extends OncePerRequestFilter {

    public static final String REDIRECT_URI_COOKIE = "hireconnect_redirect_uri";
    public static final String ROLE_COOKIE = "hireconnect_requested_role";

    private final int cookieExpirySeconds;

    public OAuth2AuthorizationRequestFilter(@Value("${app.oauth2.cookie-expiry-seconds}") int cookieExpirySeconds) {
        this.cookieExpirySeconds = cookieExpirySeconds;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/oauth2/authorization/")) {
            String redirectUri = request.getParameter("redirect_uri");
            if (isValidRedirectUri(redirectUri)) {
                CookieUtils.addCookie(response, REDIRECT_URI_COOKIE, redirectUri, cookieExpirySeconds);
            }
            String requestedRole = request.getParameter("role");
            if (StringUtils.hasText(requestedRole)) {
                CookieUtils.addCookie(response, ROLE_COOKIE, requestedRole.toUpperCase(), cookieExpirySeconds);
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isValidRedirectUri(String redirectUri) {
        return StringUtils.hasText(redirectUri)
            && (redirectUri.startsWith("http://") || redirectUri.startsWith("https://"));
    }
}
