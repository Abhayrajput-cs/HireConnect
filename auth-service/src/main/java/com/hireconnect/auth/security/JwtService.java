package com.hireconnect.auth.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.hireconnect.auth.config.JwtProperties;
import com.hireconnect.auth.domain.UserCredential;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String USER_ID_CLAIM = "uid";
    private static final String ROLE_CLAIM = "role";
    private static final String PROVIDER_CLAIM = "provider";

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(resolveSecret(properties.secret()));
    }

    public String generateAccessToken(UserCredential user) {
        return buildToken(user, properties.accessTokenTtl().toSeconds(), "access");
    }

    public String generateRefreshToken(UserCredential user) {
        return buildToken(user, properties.refreshTokenTtl().toSeconds(), "refresh");
    }

    public Claims parseClaims(String token) {
        return parseSignedClaims(token).getPayload();
    }

    public boolean isAccessToken(String token) {
        return "access".equals(parseClaims(token).get(TOKEN_TYPE_CLAIM, String.class));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(parseClaims(token).get(TOKEN_TYPE_CLAIM, String.class));
    }

    public Integer extractUserId(String token) {
        return parseClaims(token).get(USER_ID_CLAIM, Integer.class);
    }

    public Instant extractExpiration(String token) {
        return parseClaims(token).getExpiration().toInstant();
    }

    public Instant extractIssuedAt(String token) {
        return parseClaims(token).getIssuedAt().toInstant();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).isBefore(Instant.now());
    }

    public Claims safeParseClaims(String token) {
        try {
            return parseClaims(token);
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    public boolean isTokenIssuedBefore(String token, Instant cutoff) {
        if (cutoff == null) {
            return false;
        }
        Instant issuedAt = extractIssuedAt(token);
        return !issuedAt.isAfter(cutoff);
    }

    private String buildToken(UserCredential user, long ttlSeconds, String tokenType) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(ttlSeconds);
        return Jwts.builder()
            .issuer(properties.issuer())
            .subject(user.getEmail())
            .claims(Map.of(
                USER_ID_CLAIM, user.getUserId(),
                ROLE_CLAIM, user.getRole(),
                PROVIDER_CLAIM, user.getProvider(),
                TOKEN_TYPE_CLAIM, tokenType
            ))
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(signingKey)
            .compact();
    }

    private Jws<Claims> parseSignedClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token);
    }

    private byte[] resolveSecret(String configuredSecret) {
        try {
            return Decoders.BASE64.decode(configuredSecret);
        } catch (IllegalArgumentException ex) {
            return Base64.getEncoder().encode(configuredSecret.getBytes(StandardCharsets.UTF_8));
        }
    }
}
