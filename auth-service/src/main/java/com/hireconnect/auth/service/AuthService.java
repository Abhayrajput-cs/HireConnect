package com.hireconnect.auth.service;

import com.hireconnect.auth.dto.AuthResponse;
import com.hireconnect.auth.dto.LoginRequest;
import com.hireconnect.auth.dto.RegisterRequest;
import com.hireconnect.auth.dto.TokenValidationRequest;
import com.hireconnect.auth.dto.TokenValidationResponse;
import com.hireconnect.auth.dto.UserSummary;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    void logout(String token);

    TokenValidationResponse validateToken(TokenValidationRequest request);

    AuthResponse refreshToken(String token);

    UserSummary getByEmail(String email);

    AuthResponse handleGithubLogin(GithubOAuthUser githubUser, String requestedRole);

    record GithubOAuthUser(
        String email,
        String login
    ) {
    }
}
