package com.hireconnect.interview;

import static org.hamcrest.Matchers.hasSize;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class InterviewServiceIntegrationTest {

    private static final String JWT_ISSUER = "hireconnect-auth-service";
    private static final String JWT_SECRET = "VGhpc0lzQVRlc3RTZWNyZXRGb3JBdXRoU2VydmljZUxvY2FsRGV2T25seTEyMzQ1Njc4OTAxMjM0NTY=";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void recruiterCanScheduleQueryAndCancelInterview() throws Exception {
        LocalDateTime scheduledAt = LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.SECONDS);

        String response = mockMvc.perform(post("/api/v1/interviews")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("recruiter@example.com", "RECRUITER"))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationId": 501,
                      "scheduledAt": "%s",
                      "mode": "ONLINE",
                      "meetLink": "https://meet.example.com/interview-501",
                      "notes": "Bring architecture discussion points."
                    }
                    """.formatted(scheduledAt)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("SCHEDULED"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode interview = objectMapper.readTree(response);
        int interviewId = interview.get("interviewId").asInt();

        mockMvc.perform(get("/api/v1/interviews/" + interviewId)
                .header(HttpHeaders.AUTHORIZATION, bearerToken("recruiter@example.com", "RECRUITER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.applicationId").value(501));

        mockMvc.perform(get("/api/v1/interviews/application/501")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("recruiter@example.com", "RECRUITER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/v1/interviews/status/SCHEDULED")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("recruiter@example.com", "RECRUITER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].interviewId").value(interviewId));

        mockMvc.perform(get("/api/v1/interviews")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("recruiter@example.com", "RECRUITER"))
                .param("scheduledFrom", scheduledAt.minusHours(1).toString())
                .param("scheduledTo", scheduledAt.plusHours(1).toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].interviewId").value(interviewId));

        mockMvc.perform(delete("/api/v1/interviews/" + interviewId)
                .header(HttpHeaders.AUTHORIZATION, bearerToken("recruiter@example.com", "RECRUITER")))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/interviews/" + interviewId)
                .header(HttpHeaders.AUTHORIZATION, bearerToken("recruiter@example.com", "RECRUITER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    void candidateCanConfirmAndRequestReschedule() throws Exception {
        LocalDateTime scheduledAt = LocalDateTime.now().plusDays(3).truncatedTo(ChronoUnit.SECONDS);
        String response = mockMvc.perform(post("/api/v1/interviews")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("recruiter@example.com", "RECRUITER"))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationId": 502,
                      "scheduledAt": "%s",
                      "mode": "IN_PERSON",
                      "location": "Pune Office - Tower A",
                      "notes": "Meet at reception."
                    }
                    """.formatted(scheduledAt)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        int interviewId = objectMapper.readTree(response).get("interviewId").asInt();

        mockMvc.perform(patch("/api/v1/interviews/" + interviewId + "/confirm")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("candidate@example.com", "CANDIDATE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("Interview confirmed successfully"));

        LocalDateTime requestedTime = LocalDateTime.now().plusDays(4).truncatedTo(ChronoUnit.SECONDS);
        mockMvc.perform(patch("/api/v1/interviews/" + interviewId + "/reschedule")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("candidate@example.com", "CANDIDATE"))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "scheduledAt": "%s",
                      "notes": "Requesting a later slot."
                    }
                    """.formatted(requestedTime)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RESCHEDULE_REQUESTED"))
            .andExpect(jsonPath("$.scheduledAt").value(requestedTime.toString()));
    }

    @Test
    void invalidApplicationStatesAndForeignCandidateAccessAreRejected() throws Exception {
        LocalDateTime scheduledAt = LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.SECONDS);

        mockMvc.perform(post("/api/v1/interviews")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("recruiter@example.com", "RECRUITER"))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationId": 503,
                      "scheduledAt": "%s",
                      "mode": "ONLINE",
                      "meetLink": "https://meet.example.com/invalid"
                    }
                    """.formatted(scheduledAt)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Interviews can only be scheduled for SHORTLISTED or INTERVIEW_SCHEDULED applications"));

        String response = mockMvc.perform(post("/api/v1/interviews")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("recruiter@example.com", "RECRUITER"))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationId": 504,
                      "scheduledAt": "%s",
                      "mode": "ONLINE",
                      "meetLink": "https://meet.example.com/interview-504"
                    }
                    """.formatted(scheduledAt.plusDays(1))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        int interviewId = objectMapper.readTree(response).get("interviewId").asInt();

        mockMvc.perform(patch("/api/v1/interviews/" + interviewId + "/confirm")
                .header(HttpHeaders.AUTHORIZATION, bearerToken("candidate@example.com", "CANDIDATE")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Candidates can only manage their own interviews"));
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
            applications.put(503, new ApplicationSnapshot(503, 701, 101, "APPLIED"));
            applications.put(504, new ApplicationSnapshot(504, 701, 111, "INTERVIEW_SCHEDULED"));
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
                    if ("othercandidate@example.com".equalsIgnoreCase(email)) {
                        return new ProfileSnapshot(111, "CANDIDATE", email);
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
                    if (profileId == 111) {
                        return new ProfileSnapshot(111, "CANDIDATE", "othercandidate@example.com");
                    }
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Profile not found with id: " + profileId);
                }
            };
        }
    }
}
