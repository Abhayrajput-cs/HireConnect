package com.hireconnect.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.hireconnect.auth.dto.AuthResponse;
import com.hireconnect.auth.service.AuthService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final String defaultSuccessUrl;

    public OAuth2AuthenticationSuccessHandler(
        AuthService authService,
        @Value("${app.oauth2.default-success-url}") String defaultSuccessUrl
    ) {
        this.authService = authService;
        this.defaultSuccessUrl = defaultSuccessUrl;
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String email = principal.getAttribute("email");
        String login = principal.getAttribute("login");
        Object id = principal.getAttribute("id");

        AuthResponse authResponse = authService.handleGithubLogin(
            new AuthService.GithubOAuthUser(email, login),
            resolveRequestedRole(request)
        );

        String targetUrl = UriComponentsBuilder.fromUriString(resolveRedirectUri(request))
            .queryParam("accessToken", authResponse.accessToken())
            .queryParam("refreshToken", authResponse.refreshToken())
            .queryParam("role", authResponse.user().role())
            .queryParam("email", authResponse.user().email())
            .build(true)
            .toUriString();

        clearCookies(request, response);
        response.sendRedirect(targetUrl);
    }

    private String resolveRequestedRole(HttpServletRequest request) {
        var roleCookie = CookieUtils.getCookie(request, OAuth2AuthorizationRequestFilter.ROLE_COOKIE);
        if (roleCookie == null || roleCookie.getValue() == null) {
            return "CANDIDATE";
        }
        return roleCookie.getValue().toUpperCase();
    }

    private String resolveRedirectUri(HttpServletRequest request) {
        var redirectCookie = CookieUtils.getCookie(request, OAuth2AuthorizationRequestFilter.REDIRECT_URI_COOKIE);
        return redirectCookie == null ? defaultSuccessUrl : redirectCookie.getValue();
    }

    private void clearCookies(HttpServletRequest request, HttpServletResponse response) {
        CookieUtils.deleteCookie(request, response, OAuth2AuthorizationRequestFilter.REDIRECT_URI_COOKIE);
        CookieUtils.deleteCookie(request, response, OAuth2AuthorizationRequestFilter.ROLE_COOKIE);
    }
}
