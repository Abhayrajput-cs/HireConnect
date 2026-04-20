package com.hireconnect.auth.service;

import java.time.Instant;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.hireconnect.auth.config.JwtProperties;
import com.hireconnect.auth.domain.UserCredential;
import com.hireconnect.auth.dto.AuthResponse;
import com.hireconnect.auth.dto.LoginRequest;
import com.hireconnect.auth.dto.RegisterRequest;
import com.hireconnect.auth.dto.TokenValidationRequest;
import com.hireconnect.auth.dto.TokenValidationResponse;
import com.hireconnect.auth.dto.UserSummary;
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

    public AuthServiceImpl(
        AuthRepository authRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        JwtProperties jwtProperties
    ) {
        this.authRepository = authRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        String role = normalizeRole(request.role());

        if (authRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already registered");
        }

        UserCredential user = new UserCredential();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setProvider(LOCAL_PROVIDER);

        return issueTokens(authRepository.save(user));
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

        return issueTokens(user);
    }

    @Override
    public void logout(String token) {
        if (StringUtils.hasText(token) && jwtService.safeParseClaims(token) == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Token is invalid");
        }
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
                created.setPasswordHash("");
                created.setRole(normalizeRole(requestedRole));
                created.setProvider(GITHUB_PROVIDER);
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
            user.getEmail(),
            user.getRole(),
            user.getProvider(),
            user.getCreatedAt()
        );
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
