package com.hireconnect.notification;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireconnect.notification.client.ApplicationServiceClient;
import com.hireconnect.notification.client.ApplicationSnapshot;
import com.hireconnect.notification.client.JobServiceClient;
import com.hireconnect.notification.client.JobSnapshot;
import com.hireconnect.notification.client.ProfileServiceClient;
import com.hireconnect.notification.client.ProfileSnapshot;
import com.hireconnect.notification.security.AuthValidationClient;
import com.hireconnect.notification.security.AuthenticatedUser;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@SpringBootTest(properties = "app.security.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationServiceIntegrationTest {

    private static final String JWT_ISSUER = "hireconnect-auth-service";
    private static final String JWT_SECRET = "VGhpc0lzQVRlc3RTZWNyZXRGb3JBdXRoU2VydmljZUxvY2FsRGV2T25seTEyMzQ1Njc4OTAxMjM0NTY=";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void notificationLifecycleWorksForAuthenticatedOwner() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/events")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("recruiter@example.com", "RECRUITER"))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "eventType": "APPLICATION_STATUS_CHANGED",
                      "notificationType": "APPLICATION",
                      "message": "Your application moved to SHORTLISTED",
                      "recipientUserIds": [101],
                      "recipientEmails": ["candidate@example.com"],
                      "applicationId": 501,
                      "jobId": 701,
                      "recruiterId": 401,
                      "candidateId": 101,
                      "status": "SHORTLISTED",
                      "appliedAt": "2026-04-20",
                      "occurredAt": "2026-04-22T11:00:00"
                    }
                    """))
            .andExpect(status().isAccepted());

        String response = mockMvc.perform(get("/api/v1/notifications/user/101")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("candidate@example.com", "CANDIDATE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].message").value("Your application moved to SHORTLISTED"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode notification = objectMapper.readTree(response).get(0);
        int notificationId = notification.get("notificationId").asInt();

        mockMvc.perform(get("/api/v1/notifications/user/101/unread-count")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("candidate@example.com", "CANDIDATE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(1));

        mockMvc.perform(patch("/api/v1/notifications/" + notificationId + "/read")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("candidate@example.com", "CANDIDATE")))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/notifications/user/101/unread-count")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("candidate@example.com", "CANDIDATE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(0));

        mockMvc.perform(delete("/api/v1/notifications/" + notificationId)
                .header(HttpHeaders.AUTHORIZATION, bearerToken("candidate@example.com", "CANDIDATE")))
            .andExpect(status().isNoContent());
    }

    @Test
    void analyticsEndpointsReturnMergedMetrics() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/events")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("recruiter@example.com", "RECRUITER"))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "eventType": "APPLICATION_STATUS_CHANGED",
                      "notificationType": "APPLICATION",
                      "message": "Application submitted",
                      "recipientUserIds": [101],
                      "recipientEmails": ["candidate@example.com"],
                      "applicationId": 501,
                      "jobId": 701,
                      "recruiterId": 401,
                      "candidateId": 101,
                      "status": "APPLIED",
                      "appliedAt": "2026-04-20",
                      "occurredAt": "2026-04-20T09:00:00"
                    }
                    """))
            .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/v1/notifications/events")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("recruiter@example.com", "RECRUITER"))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "eventType": "APPLICATION_STATUS_CHANGED",
                      "notificationType": "APPLICATION",
                      "message": "Application offered",
                      "recipientUserIds": [101],
                      "recipientEmails": ["candidate@example.com"],
                      "applicationId": 501,
                      "jobId": 701,
                      "recruiterId": 401,
                      "candidateId": 101,
                      "status": "OFFERED",
                      "appliedAt": "2026-04-20",
                      "occurredAt": "2026-04-24T09:00:00"
                    }
                    """))
            .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/v1/analytics/jobs/701/views")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("candidate@example.com", "CANDIDATE"))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(1));

        mockMvc.perform(post("/api/v1/analytics/jobs/701/views")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("candidate@example.com", "CANDIDATE"))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(2));

        mockMvc.perform(get("/api/v1/analytics/jobs/701/view-count")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("candidate@example.com", "CANDIDATE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(2));

        mockMvc.perform(get("/api/v1/analytics/jobs/701/application-count")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("candidate@example.com", "CANDIDATE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(1));

        mockMvc.perform(get("/api/v1/analytics/jobs/701/view-to-apply-ratio")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("candidate@example.com", "CANDIDATE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(2.0));

        mockMvc.perform(get("/api/v1/analytics/recruiter/401")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("recruiter@example.com", "RECRUITER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalJobs").value(1))
            .andExpect(jsonPath("$.totalApplications").value(1))
            .andExpect(jsonPath("$.offeredCount").value(1))
            .andExpect(jsonPath("$.rejectedCount").value(0));

        mockMvc.perform(get("/api/v1/analytics/recruiter/401/time-to-hire")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("recruiter@example.com", "RECRUITER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(4.0));

        mockMvc.perform(get("/api/v1/analytics/categories/top")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("recruiter@example.com", "RECRUITER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.Engineering").value(1));
    }

    @Test
    void unauthenticatedProtectedRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/user/101"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/analytics/recruiter/401"))
            .andExpect(status().isUnauthorized());
    }

    private String bearerToken(String email, String role) {
        return "Bearer " + Jwts.builder()
            .issuer(JWT_ISSUER)
            .subject(email)
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
            return new AuthValidationClient(new org.springframework.web.client.RestTemplate(), "http://localhost:8081") {
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
        ProfileServiceClient profileServiceClient() {
            return new ProfileServiceClient(new org.springframework.web.client.RestTemplate(), "http://localhost:8084") {
                @Override
                public ProfileSnapshot getProfileByEmail(String email) {
                    if ("candidate@example.com".equalsIgnoreCase(email)) {
                        return new ProfileSnapshot(101, "CANDIDATE", "Demo Candidate", email);
                    }
                    if ("recruiter@example.com".equalsIgnoreCase(email)) {
                        return new ProfileSnapshot(401, "RECRUITER", "Demo Recruiter", email);
                    }
                    if ("admin@example.com".equalsIgnoreCase(email)) {
                        return new ProfileSnapshot(999, "ADMIN", "Platform Admin", email);
                    }
                    return null;
                }

                @Override
                public ProfileSnapshot getProfileById(Integer profileId) {
                    return switch (profileId) {
                        case 101 -> new ProfileSnapshot(101, "CANDIDATE", "Demo Candidate", "candidate@example.com");
                        case 401 -> new ProfileSnapshot(401, "RECRUITER", "Demo Recruiter", "recruiter@example.com");
                        default -> null;
                    };
                }

                @Override
                public List<ProfileSnapshot> getProfilesByRole(String role) {
                    if ("CANDIDATE".equalsIgnoreCase(role)) {
                        return List.of(new ProfileSnapshot(101, "CANDIDATE", "Demo Candidate", "candidate@example.com"));
                    }
                    if ("RECRUITER".equalsIgnoreCase(role)) {
                        return List.of(new ProfileSnapshot(401, "RECRUITER", "Demo Recruiter", "recruiter@example.com"));
                    }
                    return List.of();
                }
            };
        }

      

        @Bean
        @Primary
        ApplicationServiceClient applicationServiceClient() {
            return new ApplicationServiceClient(new org.springframework.web.client.RestTemplate(), "http://localhost:8086") {
                @Override
                public List<ApplicationSnapshot> getApplicationsByJob(Integer jobId) {
                    return List.of(
                        new ApplicationSnapshot(501, 701, 101, LocalDate.of(2026, 4, 20), "OFFERED"),
                        new ApplicationSnapshot(502, 701, 102, LocalDate.of(2026, 4, 21), "REJECTED")
                    );
                }

                @Override
                public int getApplicationCountByJob(Integer jobId) {
                    return 2;
                }

                @Override
                public ApplicationSnapshot getApplication(Integer applicationId) {
                    return new ApplicationSnapshot(applicationId, 701, 101, LocalDate.of(2026, 4, 20), "OFFERED");
                }
            };
        }
    }
}
