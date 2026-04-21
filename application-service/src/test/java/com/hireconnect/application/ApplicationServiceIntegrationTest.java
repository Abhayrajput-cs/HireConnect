package com.hireconnect.application;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireconnect.application.client.CandidateDirectoryClient;
import com.hireconnect.application.client.CandidateProfileSnapshot;
import com.hireconnect.application.client.JobCatalogClient;
import com.hireconnect.application.client.JobSnapshot;
import com.hireconnect.application.exception.ApiException;

import org.springframework.http.HttpStatus;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ApplicationServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void applicationLifecycleWorks() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "jobId": 701,
                      "candidateId": 101,
                      "coverLetter": "I am excited to join the backend team.",
                      "resumeUrl": "https://files.example.com/resume/candidate-101.pdf"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("APPLIED"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        int applicationId = objectMapper.readTree(createResponse).get("applicationId").asInt();

        mockMvc.perform(get("/api/v1/applications/" + applicationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jobId").value(701))
            .andExpect(jsonPath("$.candidateId").value(101));

        mockMvc.perform(get("/api/v1/applications/candidate/101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/v1/applications/job/701"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].applicationId").value(applicationId));

        mockMvc.perform(get("/api/v1/applications/job/701/count"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(1));

        mockMvc.perform(patch("/api/v1/applications/" + applicationId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "SHORTLISTED"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SHORTLISTED"));

        mockMvc.perform(get("/api/v1/applications").param("status", "SHORTLISTED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].applicationId").value(applicationId));
    }

    @Test
    void duplicateApplicationsAndInvalidStatusTransitionsAreRejected() throws Exception {
        mockMvc.perform(post("/api/v1/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "jobId": 701,
                      "candidateId": 101,
                      "coverLetter": "First attempt",
                      "resumeUrl": "https://files.example.com/resume/candidate-101.pdf"
                    }
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "jobId": 701,
                      "candidateId": 101,
                      "coverLetter": "Duplicate attempt",
                      "resumeUrl": "https://files.example.com/resume/candidate-101.pdf"
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Candidate has already applied to this job"));

        String createResponse = mockMvc.perform(post("/api/v1/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "jobId": 702,
                      "candidateId": 101,
                      "coverLetter": "Transition test",
                      "resumeUrl": "https://files.example.com/resume/candidate-101.pdf"
                    }
                    """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        int applicationId = objectMapper.readTree(createResponse).get("applicationId").asInt();

        mockMvc.perform(patch("/api/v1/applications/" + applicationId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "OFFERED"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid application status transition from APPLIED to OFFERED"));
    }

    @Test
    void withdrawalAndDateRangeQueriesWork() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "jobId": 703,
                      "candidateId": 101,
                      "coverLetter": "Ready to withdraw later",
                      "resumeUrl": "https://files.example.com/resume/candidate-101.pdf"
                    }
                    """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode application = objectMapper.readTree(createResponse);
        int applicationId = application.get("applicationId").asInt();
        String appliedAt = application.get("appliedAt").asText();

        mockMvc.perform(get("/api/v1/applications")
                .param("appliedFrom", appliedAt)
                .param("appliedTo", appliedAt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].applicationId", hasItem(applicationId)));

        mockMvc.perform(patch("/api/v1/applications/" + applicationId + "/withdraw"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("WITHDRAWN"));
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
                    if (candidateId == 202) {
                        return new CandidateProfileSnapshot(202, "RECRUITER", "recruiter@example.com", null);
                    }
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Candidate profile not found with id: " + candidateId);
                }

                @Override
                public CandidateProfileSnapshot getCandidateProfileByEmail(String email) {
                    return new CandidateProfileSnapshot(101, "CANDIDATE", email, "https://files.example.com/resume/candidate-101.pdf");
                }
            };
        }

        @Bean
        @Primary
        JobCatalogClient jobCatalogClient() {
            return jobId -> {
                if (jobId == 701 || jobId == 702 || jobId == 703) {
                    return new JobSnapshot(jobId, 401, "OPEN", "Backend Engineer");
                }
                if (jobId == 704) {
                    return new JobSnapshot(704, 401, "PAUSED", "Paused Job");
                }
                throw new ApiException(HttpStatus.BAD_REQUEST, "Job not found with id: " + jobId);
            };
        }
    }
}
