package com.hireconnect.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hireconnect.auth.dto.AuthResponse;
import com.hireconnect.auth.dto.ForgotPasswordRequest;
import com.hireconnect.auth.dto.LoginRequest;
import com.hireconnect.auth.dto.MessageResponse;
import com.hireconnect.auth.dto.RegistrationResponse;
import com.hireconnect.auth.dto.RefreshTokenRequest;
import com.hireconnect.auth.dto.RegisterRequest;
import com.hireconnect.auth.dto.ResendVerificationRequest;
import com.hireconnect.auth.dto.ResetPasswordRequest;
import com.hireconnect.auth.dto.TokenValidationRequest;
import com.hireconnect.auth.dto.TokenValidationResponse;
import com.hireconnect.auth.dto.UserSummary;
import com.hireconnect.auth.dto.VerifyEmailRequest;
import com.hireconnect.auth.service.AuthService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/auth")
public class AuthResource {

    private final AuthService authService;

    public AuthResource(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/verify-email")
    public AuthResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return authService.verifyEmail(request);
    }

    @PostMapping("/resend-verification")
    public RegistrationResponse resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        return authService.resendVerification(request);
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
    ) {
        authService.logout(extractBearerToken(authorizationHeader));
        return ResponseEntity.ok("Logout processed successfully");
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refreshToken(request.refreshToken());
    }

    @PostMapping("/validate")
    public TokenValidationResponse validate(@Valid @RequestBody TokenValidationRequest request) {
        return authService.validateToken(request);
    }

    @GetMapping("/me")
    public UserSummary me(Authentication authentication) {
        return authService.getByEmail(authentication.getName());
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring(7);
    }
}
