package com.hireconnect.auth.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.hireconnect.auth.config.JwtProperties;
import com.hireconnect.auth.domain.UserCredential;
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
import com.hireconnect.auth.exception.ApiException;
import com.hireconnect.auth.repository.AuthRepository;
import com.hireconnect.auth.security.JwtService;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String LOCAL_PROVIDER = "LOCAL";
    private static final String GITHUB_PROVIDER = "GITHUB";

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final EmailVerificationService emailVerificationService;

    public AuthServiceImpl(
        AuthRepository authRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        JwtProperties jwtProperties,
        EmailVerificationService emailVerificationService
    ) {
        this.authRepository = authRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.emailVerificationService = emailVerificationService;
    }

    @Override
    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        String fullName = normalizeFullName(request.fullName());
        String role = normalizeRole(request.role());

        if (authRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already registered");
        }

        UserCredential user = new UserCredential();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setProvider(LOCAL_PROVIDER);
        emailVerificationService.prepareVerification(user);

        UserCredential saved = authRepository.save(user);
        emailVerificationService.sendVerification(saved);
        return new RegistrationResponse(
            saved.getEmail(),
            saved.getRole(),
            !saved.isEmailVerified(),
            saved.isEmailVerified()
                ? "Account created successfully"
                : "Verification code sent to your email. Verify before login."
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserCredential user = authRepository.findByEmail(normalizeEmail(request.email()))
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!LOCAL_PROVIDER.equals(user.getProvider())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "This account uses GitHub OAuth. Use GitHub sign-in instead.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        if (!user.isEmailVerified()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Please verify your email before signing in");
        }

        return issueTokens(user);
    }

    @Override
    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        UserCredential user = authRepository.findByEmail(normalizeEmail(request.email()))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found for this email"));
        if (user.isEmailVerified()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email is already verified. Please sign in.");
        }
        if (!StringUtils.hasText(user.getEmailVerificationCode())
            || user.getEmailVerificationExpiresAt() == null
            || user.getEmailVerificationExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Verification code expired. Request a new code.");
        }
        if (!user.getEmailVerificationCode().equals(request.code().trim())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid verification code");
        }
        user.setEmailVerified(true);
        user.setEmailVerificationCode(null);
        user.setEmailVerificationExpiresAt(null);
        return issueTokens(authRepository.save(user));
    }

    @Override
    @Transactional
    public RegistrationResponse resendVerification(ResendVerificationRequest request) {
        UserCredential user = authRepository.findByEmail(normalizeEmail(request.email()))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found for this email"));
        if (user.isEmailVerified()) {
            return new RegistrationResponse(user.getEmail(), user.getRole(), false, "Email is already verified");
        }
        emailVerificationService.prepareVerification(user);
        UserCredential saved = authRepository.save(user);
        emailVerificationService.sendVerification(saved);
        return new RegistrationResponse(saved.getEmail(), saved.getRole(), true, "Verification code resent to your email");
    }

    @Override
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        UserCredential user = authRepository.findByEmail(normalizeEmail(request.email()))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found for this email"));
        if (!LOCAL_PROVIDER.equals(user.getProvider())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This account uses GitHub OAuth. Use GitHub sign-in instead.");
        }
        if (!user.isEmailVerified()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Please verify your email before resetting your password");
        }
        emailVerificationService.preparePasswordReset(user);
        UserCredential saved = authRepository.save(user);
        emailVerificationService.sendPasswordReset(saved);
        return new MessageResponse("Password reset code sent to your verified email");
    }

    @Override
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        UserCredential user = authRepository.findByEmail(normalizeEmail(request.email()))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found for this email"));
        if (!StringUtils.hasText(user.getPasswordResetCode())
            || user.getPasswordResetExpiresAt() == null
            || user.getPasswordResetExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password reset code expired. Request a new code.");
        }
        if (!user.getPasswordResetCode().equals(request.code().trim())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid password reset code");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordResetCode(null);
        user.setPasswordResetExpiresAt(null);
        user.setTokensInvalidBefore(Instant.now());
        authRepository.save(user);
        return new MessageResponse("Password updated successfully. Please sign in again.");
    }

    @Override
    @Transactional
    public void logout(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }

        var claims = jwtService.safeParseClaims(token);
        if (claims == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Token is invalid");
        }

        Integer userId = claims.get("uid", Integer.class);
        UserCredential user = authRepository.findByUserId(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User account not found"));

        user.setTokensInvalidBefore(Instant.now());
        authRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenValidationResponse validateToken(TokenValidationRequest request) {
        var claims = jwtService.safeParseClaims(request.token());
        if (claims == null || !jwtService.isAccessToken(request.token())) {
            return new TokenValidationResponse(false, null, null, null, null, null, "Token is invalid or not an access token");
        }

        Integer userId = claims.get("uid", Integer.class);
        UserCredential user = authRepository.findByUserId(userId).orElse(null);
        if (user == null) {
            return new TokenValidationResponse(false, null, null, null, null, null, "User account no longer exists");
        }

        if (jwtService.isTokenIssuedBefore(request.token(), user.getTokensInvalidBefore())) {
            return new TokenValidationResponse(false, null, null, null, null, null, "Token has been invalidated by logout");
        }

        return new TokenValidationResponse(
            true,
            user.getUserId(),
            user.getEmail(),
            user.getRole(),
            user.getProvider(),
            claims.getExpiration().toInstant(),
            "Token is valid"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String token) {
        var claims = jwtService.safeParseClaims(token);
        if (claims == null || !jwtService.isRefreshToken(token)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid");
        }

        Integer userId = claims.get("uid", Integer.class);
        UserCredential user = authRepository.findByUserId(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User account not found"));

        if (jwtService.isTokenIssuedBefore(token, user.getTokensInvalidBefore())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token has been invalidated by logout");
        }

        return issueTokens(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummary getByEmail(String email) {
        UserCredential user = authRepository.findByEmail(normalizeEmail(email))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        return toUserSummary(user);
    }

    @Override
    @Transactional
    public AuthResponse handleGithubLogin(GithubOAuthUser githubUser, String requestedRole) {
        String email = resolveGithubEmail(githubUser);
        UserCredential user = authRepository.findByEmail(email)
            .map(existing -> {
                if (LOCAL_PROVIDER.equals(existing.getProvider())) {
                    throw new ApiException(HttpStatus.CONFLICT, "Email is already registered with email/password. Use local login.");
                }
                return existing;
            })
            .orElseGet(() -> {
                UserCredential created = new UserCredential();
                created.setEmail(email);
                created.setFullName(resolveGithubName(githubUser));
                created.setPasswordHash("");
                created.setRole(normalizeRole(requestedRole));
                created.setProvider(GITHUB_PROVIDER);
                created.setEmailVerified(true);
                return authRepository.save(created);
            });

        return issueTokens(user);
    }

    private String resolveGithubEmail(GithubOAuthUser githubUser) {
        if (StringUtils.hasText(githubUser.email())) {
            return normalizeEmail(githubUser.email());
        }
        if (StringUtils.hasText(githubUser.login())) {
            return normalizeEmail(githubUser.login() + "@users.noreply.github.com");
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "GitHub profile did not include a usable identifier");
    }

    private AuthResponse issueTokens(UserCredential user) {
        Instant now = Instant.now();
        return new AuthResponse(
            jwtService.generateAccessToken(user),
            jwtService.generateRefreshToken(user),
            "Bearer",
            now.plus(jwtProperties.accessTokenTtl()),
            now.plus(jwtProperties.refreshTokenTtl()),
            toUserSummary(user)
        );
    }

    private UserSummary toUserSummary(UserCredential user) {
        return new UserSummary(
            user.getUserId(),
            user.getFullName(),
            user.getEmail(),
            user.getRole(),
            user.getProvider(),
            user.getCreatedAt()
        );
    }

    private String normalizeFullName(String fullName) {
        String normalized = fullName == null ? "" : fullName.trim().replaceAll("\\s+", " ");
        if (!StringUtils.hasText(normalized)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Full name is required");
        }
        return normalized;
    }

    private String resolveGithubName(GithubOAuthUser githubUser) {
        if (StringUtils.hasText(githubUser.name())) {
            return normalizeFullName(githubUser.name());
        }
        if (StringUtils.hasText(githubUser.login())) {
            return normalizeFullName(githubUser.login());
        }
        return "GitHub User";
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRole(String role) {
        String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
        if (!"CANDIDATE".equals(normalizedRole) && !"RECRUITER".equals(normalizedRole)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Role must be CANDIDATE or RECRUITER");
        }
        return normalizedRole;
    }
}
