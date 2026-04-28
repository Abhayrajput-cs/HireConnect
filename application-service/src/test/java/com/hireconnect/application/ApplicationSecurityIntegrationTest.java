package com.hireconnect.application;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.hireconnect.application.client.CandidateDirectoryClient;
import com.hireconnect.application.client.CandidateProfileSnapshot;
import com.hireconnect.application.client.JobCatalogClient;
import com.hireconnect.application.client.JobSnapshot;
import com.hireconnect.application.exception.ApiException;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@SpringBootTest(properties = "app.security.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ApplicationSecurityIntegrationTest {

    private static final String JWT_ISSUER = "hireconnect-auth-service";
    private static final String JWT_SECRET = "VGhpc0lzQVRlc3RTZWNyZXRGb3JBdXRoU2VydmljZUxvY2FsRGV2T25seTEyMzQ1Njc4OTAxMjM0NTY=";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/applications/candidate/101"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validApplicationPayload()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void candidateCanSubmitAndWithdrawButCannotReadJobLevelViews() throws Exception {
        String response = mockMvc.perform(post("/api/v1/applications")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("candidate@example.com", "CANDIDATE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validApplicationPayload()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("APPLIED"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        int applicationId = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
            .readTree(response)
            .get("applicationId")
            .asInt();

        mockMvc.perform(get("/api/v1/applications/candidate/101")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("candidate@example.com", "CANDIDATE")))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/applications/job/701")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("candidate@example.com", "CANDIDATE")))
            .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/applications/" + applicationId + "/withdraw")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("candidate@example.com", "CANDIDATE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("WITHDRAWN"));
    }

    @Test
    void recruiterCanUpdateStatusButCannotSubmitCandidateApplications() throws Exception {
        String response = mockMvc.perform(post("/api/v1/applications")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("candidate@example.com", "CANDIDATE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validApplicationPayload()))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        int applicationId = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
            .readTree(response)
            .get("applicationId")
            .asInt();

        mockMvc.perform(patch("/api/v1/applications/" + applicationId + "/status")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("recruiter@example.com", "RECRUITER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "SHORTLISTED"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SHORTLISTED"));

        mockMvc.perform(post("/api/v1/applications")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("recruiter@example.com", "RECRUITER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validApplicationPayload()))
            .andExpect(status().isForbidden());
    }

    private String validApplicationPayload() {
        return """
            {
              "jobId": 701,
              "candidateId": 101,
              "coverLetter": "Security test",
              "resumeUrl": "https://files.example.com/resume/candidate-101.pdf"
            }
            """;
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
        CandidateDirectoryClient candidateDirectoryClient() {
            return new CandidateDirectoryClient() {
                @Override
                public CandidateProfileSnapshot getCandidateProfile(Integer candidateId) {
                    if (candidateId == 101) {
                        return new CandidateProfileSnapshot(101, "CANDIDATE", "candidate@example.com", "https://files.example.com/resume/candidate-101.pdf");
                    }
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Candidate profile not found with id: " + candidateId);
                }

                @Override
                public CandidateProfileSnapshot getCandidateProfileByEmail(String email) {
                    if ("candidate@example.com".equalsIgnoreCase(email)) {
                        return new CandidateProfileSnapshot(101, "CANDIDATE", email, "https://files.example.com/resume/candidate-101.pdf");
                    }
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Candidate profile not found for authenticated user");
                }
            };
        }

        @Bean
        @Primary
        JobCatalogClient jobCatalogClient() {
            return jobId -> {
                if (jobId == 701) {
                    return new JobSnapshot(701, 401, "OPEN", "Backend Engineer");
                }
                throw new ApiException(HttpStatus.BAD_REQUEST, "Job not found with id: " + jobId);
            };
        }
    }
}
