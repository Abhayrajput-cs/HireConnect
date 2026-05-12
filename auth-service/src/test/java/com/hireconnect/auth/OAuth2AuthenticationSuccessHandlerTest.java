package com.hireconnect.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ActiveProfiles;

import com.hireconnect.auth.repository.AuthRepository;
import com.hireconnect.auth.security.OAuth2AuthenticationSuccessHandler;
import com.hireconnect.auth.security.OAuth2AuthorizationRequestFilter;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@ActiveProfiles("test")
class OAuth2AuthenticationSuccessHandlerTest {

    @Autowired
    private OAuth2AuthenticationSuccessHandler successHandler;

    @Autowired
    private AuthRepository authRepository;

    @Test
    void successHandlerCreatesGithubRecruiterAndRedirectsWithTokens() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/login/oauth2/code/github");
        request.setCookies(
            new Cookie(OAuth2AuthorizationRequestFilter.REDIRECT_URI_COOKIE, "http://localhost:3000/callback"),
            new Cookie(OAuth2AuthorizationRequestFilter.ROLE_COOKIE, "RECRUITER")
        );

        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2User oauth2User = new DefaultOAuth2User(
            Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")),
            Map.of(
                "id", 789L,
                "login", "hireconnect-recruiter",
                "email", "oauth.recruiter@example.com"
            ),
            "login"
        );
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
            oauth2User,
            oauth2User.getAuthorities(),
            "github"
        );

        successHandler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl()).isNotBlank();
        URI redirect = URI.create(response.getRedirectedUrl());
        Map<String, String> queryParams = Arrays.stream(redirect.getQuery().split("&"))
            .map(part -> part.split("=", 2))
            .collect(Collectors.toMap(
                pair -> URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                pair -> pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : ""
            ));

        assertThat(redirect.toString()).startsWith("http://localhost:3000/callback");
        assertThat(queryParams.get("accessToken")).isNotBlank();
        assertThat(queryParams.get("refreshToken")).isNotBlank();
        assertThat(queryParams.get("role")).isEqualTo("RECRUITER");
        assertThat(queryParams.get("email")).isEqualTo("oauth.recruiter@example.com");

        assertThat(authRepository.findByEmail("oauth.recruiter@example.com"))
            .get()
            .satisfies(user -> assertThat(user.getProvider()).isEqualTo("GITHUB"));
    }
}
