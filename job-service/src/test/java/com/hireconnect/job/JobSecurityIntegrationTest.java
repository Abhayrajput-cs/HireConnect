package com.hireconnect.job;

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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.hireconnect.job.client.RecruiterDirectoryClient;
import com.hireconnect.job.client.RecruiterProfileSnapshot;
import com.hireconnect.job.exception.ApiException;
import com.hireconnect.job.security.AuthValidationClient;
import com.hireconnect.job.security.AuthenticatedUser;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@SpringBootTest(properties = "app.security.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JobSecurityIntegrationTest {

    private static final String JWT_ISSUER = "hireconnect-auth-service";
    private static final String JWT_SECRET = "VGhpc0lzQVRlc3RTZWNyZXRGb3JBdXRoU2VydmljZUxvY2FsRGV2T25seTEyMzQ1Njc4OTAxMjM0NTY=";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/jobs"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validJobPayload(101)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void candidateCanReadButCannotCreateJobs() throws Exception {
        mockMvc.perform(post("/api/v1/jobs")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("RECRUITER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validJobPayload(101)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.postedBy").value(101));

        mockMvc.perform(get("/api/v1/jobs")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("CANDIDATE")))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/jobs")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("CANDIDATE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validJobPayload(101)))
            .andExpect(status().isForbidden());
    }

    @Test
    void recruiterCanCreateJobs() throws Exception {
        mockMvc.perform(post("/api/v1/jobs")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("RECRUITER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validJobPayload(101)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Secure Java Developer"));
    }

    private String validJobPayload(int postedBy) {
        return """
            {
              "title": "Secure Java Developer",
              "category": "Engineering",
              "type": "Full-time",
              "location": "Pune",
              "salaryMin": 1200000,
              "salaryMax": 1800000,
              "description": "Build and scale secure backend services.",
              "skills": ["Java", "Spring Boot"],
              "experienceRequired": 4,
              "postedBy": %s,
              "status": "OPEN",
              "postedAt": "2026-04-21"
            }
            """.formatted(postedBy);
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

    @TestConfiguration
    static class TestClientConfig {

        @Bean
        @Primary
        AuthValidationClient authValidationClient() {
            return new AuthValidationClient("http://localhost:8081") {
                @Override
                public AuthenticatedUser validateAccessToken(String token) {
                    Claims claims = Jwts.parser()
                        .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET)))
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
                    return new AuthenticatedUser(claims.getSubject(), claims.get("role", String.class));
                }
            };
        }

        @Bean
        @Primary
        RecruiterDirectoryClient recruiterDirectoryClient() {
            return new RecruiterDirectoryClient() {
                @Override
                public RecruiterProfileSnapshot getRecruiterProfile(Integer profileId) {
                    if (profileId == 101) {
                        return new RecruiterProfileSnapshot(101, "RECRUITER", "Priya Verma", "priya@hireco.com");
                    }
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Recruiter profile not found with id: " + profileId);
                }

                @Override
                public java.util.List<RecruiterProfileSnapshot> getProfilesByRole(String role) {
                    return java.util.List.of();
                }
            };
        }
    }
}
