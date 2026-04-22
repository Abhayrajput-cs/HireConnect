package com.hireconnect.interview;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.hireconnect.interview.client.ApplicationCatalogClient;
import com.hireconnect.interview.client.ApplicationSnapshot;
import com.hireconnect.interview.client.JobCatalogClient;
import com.hireconnect.interview.client.JobSnapshot;
import com.hireconnect.interview.client.ProfileDirectoryClient;
import com.hireconnect.interview.client.ProfileSnapshot;
import com.hireconnect.interview.exception.ApiException;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@SpringBootTest(properties = "app.security.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class InterviewSecurityIntegrationTest {

    private static final String JWT_ISSUER = "hireconnect-auth-service";
    private static final String JWT_SECRET = "VGhpc0lzQVRlc3RTZWNyZXRGb3JBdXRoU2VydmljZUxvY2FsRGV2T25seTEyMzQ1Njc4OTAxMjM0NTY=";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/interviews/1"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/interviews")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(schedulePayload(501, LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.SECONDS))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void candidateCannotScheduleAndRecruiterCannotConfirm() throws Exception {
        LocalDateTime scheduledAt = LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.SECONDS);

        mockMvc.perform(post("/api/v1/interviews")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("candidate@example.com", "CANDIDATE"))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(schedulePayload(501, scheduledAt)))
            .andExpect(status().isForbidden());

        String response = mockMvc.perform(post("/api/v1/interviews")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("recruiter@example.com", "RECRUITER"))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(schedulePayload(501, scheduledAt.plusHours(3))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        int interviewId = JsonMapper.builder().build().readTree(response).get("interviewId").asInt();

        mockMvc.perform(patch("/api/v1/interviews/" + interviewId + "/confirm")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("recruiter@example.com", "RECRUITER")))
            .andExpect(status().isForbidden());
    }

    @Test
    void candidateCanConfirmButCannotUseRecruiterListingEndpoints() throws Exception {
        LocalDateTime scheduledAt = LocalDateTime.now().plusDays(3).truncatedTo(ChronoUnit.SECONDS);
        String response = mockMvc.perform(post("/api/v1/interviews")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("recruiter@example.com", "RECRUITER"))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(schedulePayload(502, scheduledAt)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        int interviewId = JsonMapper.builder().build().readTree(response).get("interviewId").asInt();

        mockMvc.perform(patch("/api/v1/interviews/" + interviewId + "/confirm")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("candidate@example.com", "CANDIDATE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("Interview confirmed successfully"));

        mockMvc.perform(get("/api/v1/interviews/status/CONFIRMED")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("candidate@example.com", "CANDIDATE")))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/interviews")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("candidate@example.com", "CANDIDATE"))
                .param("scheduledFrom", scheduledAt.minusHours(1).toString())
                .param("scheduledTo", scheduledAt.plusHours(1).toString()))
            .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/interviews/" + interviewId)
                .header(HttpHeaders.AUTHORIZATION, bearerToken("candidate@example.com", "CANDIDATE")))
            .andExpect(status().isForbidden());
    }

    private String schedulePayload(Integer applicationId, LocalDateTime scheduledAt) {
        return """
            {
              "applicationId": %d,
              "scheduledAt": "%s",
              "mode": "ONLINE",
              "meetLink": "https://meet.example.com/security"
            }
            """.formatted(applicationId, scheduledAt);
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
        ApplicationCatalogClient applicationCatalogClient() {
            Map<Integer, ApplicationSnapshot> applications = new ConcurrentHashMap<>();
            applications.put(501, new ApplicationSnapshot(501, 701, 101, "SHORTLISTED"));
            applications.put(502, new ApplicationSnapshot(502, 701, 101, "INTERVIEW_SCHEDULED"));
            return new ApplicationCatalogClient() {
                @Override
                public ApplicationSnapshot getApplication(Integer applicationId) {
                    ApplicationSnapshot application = applications.get(applicationId);
                    if (application == null) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "Application not found with id: " + applicationId);
                    }
                    return application;
                }

                @Override
                public void markInterviewScheduled(Integer applicationId) {
                    ApplicationSnapshot application = getApplication(applicationId);
                    applications.put(
                        applicationId,
                        new ApplicationSnapshot(application.applicationId(), application.jobId(), application.candidateId(), "INTERVIEW_SCHEDULED")
                    );
                }
            };
        }

        @Bean
        @Primary
        JobCatalogClient jobCatalogClient() {
            return jobId -> {
                if (jobId == 701) {
                    return new JobSnapshot(701, 401, "OPEN", "Senior Java Developer");
                }
                throw new ApiException(HttpStatus.BAD_REQUEST, "Job not found with id: " + jobId);
            };
        }

        @Bean
        @Primary
        ProfileDirectoryClient profileDirectoryClient() {
            return new ProfileDirectoryClient() {
                @Override
                public ProfileSnapshot getProfileByEmail(String email) {
                    if ("recruiter@example.com".equalsIgnoreCase(email)) {
                        return new ProfileSnapshot(401, "RECRUITER", email);
                    }
                    if ("candidate@example.com".equalsIgnoreCase(email)) {
                        return new ProfileSnapshot(101, "CANDIDATE", email);
                    }
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Profile not found for authenticated user");
                }

                @Override
                public ProfileSnapshot getProfileById(Integer profileId) {
                    if (profileId == 401) {
                        return new ProfileSnapshot(401, "RECRUITER", "recruiter@example.com");
                    }
                    if (profileId == 101) {
                        return new ProfileSnapshot(101, "CANDIDATE", "candidate@example.com");
                    }
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Profile not found with id: " + profileId);
                }
            };
        }
    }
}
