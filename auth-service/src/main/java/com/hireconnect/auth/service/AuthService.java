package com.hireconnect.auth.service;

import com.hireconnect.auth.dto.AuthResponse;
import com.hireconnect.auth.dto.ForgotPasswordRequest;
import com.hireconnect.auth.dto.LoginRequest;
import com.hireconnect.auth.dto.MessageResponse;
import com.hireconnect.auth.dto.RegistrationResponse;
import com.hireconnect.auth.dto.RegisterRequest;
import com.hireconnect.auth.dto.ResendVerificationRequest;
import com.hireconnect.auth.dto.ResetPasswordRequest;
import com.hireconnect.auth.dto.TokenValidationRequest;
import com.hireconnect.auth.dto.TokenValidationResponse;
import com.hireconnect.auth.dto.UserSummary;
import com.hireconnect.auth.dto.VerifyEmailRequest;

public interface AuthService {

    RegistrationResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse verifyEmail(VerifyEmailRequest request);

    RegistrationResponse resendVerification(ResendVerificationRequest request);

    MessageResponse forgotPassword(ForgotPasswordRequest request);

    MessageResponse resetPassword(ResetPasswordRequest request);

    void logout(String token);

    TokenValidationResponse validateToken(TokenValidationRequest request);

    AuthResponse refreshToken(String token);

    UserSummary getByEmail(String email);

    AuthResponse handleGithubLogin(GithubOAuthUser githubUser, String requestedRole);

    record GithubOAuthUser(
        String email,
        String login,
        String name
    ) {
    }
}
