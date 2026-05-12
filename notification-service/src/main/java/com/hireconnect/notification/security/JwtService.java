package com.hireconnect.notification.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.hireconnect.notification.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtService {

    private final JwtProperties properties;
    private final AuthValidationClient authValidationClient;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties, AuthValidationClient authValidationClient) {
        this.properties = properties;
        this.authValidationClient = authValidationClient;
        this.signingKey = Keys.hmacShaKeyFor(resolveSecret(properties.secret()));
    }

    public AuthenticatedUser resolveAuthenticatedUser(String token) {
        Claims claims = safeParseAccessToken(token);
        if (claims == null) {
            return null;
        }
        return authValidationClient.validateAccessToken(token);
    }

    private Claims safeParseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            if (!properties.issuer().equals(claims.getIssuer())) {
                return null;
            }
            if (!"access".equals(claims.get("token_type", String.class))) {
                return null;
            }
            return claims;
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    private byte[] resolveSecret(String configuredSecret) {
        try {
            return Decoders.BASE64.decode(configuredSecret);
        } catch (IllegalArgumentException ex) {
            return Base64.getEncoder().encode(configuredSecret.getBytes(StandardCharsets.UTF_8));
        }
    }
}
