package com.hireconnect.profile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@SpringBootTest(properties = "app.security.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileSecurityIntegrationTest {

    private static final String JWT_ISSUER = "hireconnect-auth-service";
    private static final String JWT_SECRET = "VGhpc0lzQVRlc3RTZWNyZXRGb3JBdXRoU2VydmljZUxvY2FsRGV2T25seTEyMzQ1Njc4OTAxMjM0NTY=";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/profiles"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/profiles/candidates")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullName": "Blocked Candidate",
                      "email": "blocked@example.com",
                      "mobile": 9999911111,
                      "skills": ["Java"],
                      "experience": 1,
                      "resumeUrl": "https://files.example.com/resume/blocked.pdf"
                    }
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanAccessProfileEndpoints() throws Exception {
        String email = "secured." + System.nanoTime() + "@example.com";

        mockMvc.perform(post("/api/v1/profiles/candidates")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("CANDIDATE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullName": "Secured Candidate",
                      "email": "%s",
                      "mobile": 9876501234,
                      "skills": ["Java", "Spring Boot"],
                      "experience": 2,
                      "resumeUrl": "https://files.example.com/resume/secured.pdf"
                    }
                    """.formatted(email)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value(email));

        mockMvc.perform(get("/api/v1/profiles/email/" + email)
                .header(HttpHeaders.AUTHORIZATION, bearerToken("CANDIDATE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email));
    }

    private String bearerToken(String role) {
        return "Bearer " + Jwts.builder()
            .issuer(JWT_ISSUER)
            .subject(role.toLowerCase() + "@example.com")
            .claims(Map.of(
                "uid", 1,
                "role", role,
                "provider", "LOCAL",
                "token_type", "access"
            ))
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
            .signWith(signingKey())
            .compact();
    }

    private SecretKey signingKey() {
        try {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET));
        } catch (IllegalArgumentException ex) {
            return Keys.hmacShaKeyFor(Base64.getEncoder().encode(JWT_SECRET.getBytes(StandardCharsets.UTF_8)));
        }
    }
}
