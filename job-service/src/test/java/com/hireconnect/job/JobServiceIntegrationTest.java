package com.hireconnect.job;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireconnect.job.client.RecruiterDirectoryClient;
import com.hireconnect.job.client.RecruiterProfileSnapshot;
import com.hireconnect.job.exception.ApiException;

import org.springframework.http.HttpStatus;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JobServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void jobCrudAndSearchFlowWorks() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Senior Java Developer",
                      "category": "Engineering",
                      "type": "Full-time",
                      "location": "Pune",
                      "salaryMin": 1200000,
                      "salaryMax": 1800000,
                      "description": "Build and scale backend services.",
                      "skills": ["Java", "Spring Boot", "MySQL"],
                      "experienceRequired": 4,
                      "postedBy": 101,
                      "status": "OPEN",
                      "postedAt": "2026-04-21"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.postedBy").value(101))
            .andExpect(jsonPath("$.skills", hasSize(3)))
            .andReturn()
            .getResponse()
            .getContentAsString();

        int jobId = objectMapper.readTree(createResponse).get("jobId").asInt();

        mockMvc.perform(get("/api/v1/jobs/" + jobId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Senior Java Developer"));

        mockMvc.perform(get("/api/v1/jobs").param("category", "Engineering").param("location", "Pune"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].jobId").value(jobId));

        mockMvc.perform(get("/api/v1/jobs/recruiter/101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].location").value("Pune"));

        mockMvc.perform(put("/api/v1/jobs/" + jobId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "location": "Bengaluru",
                      "salaryMax": 1900000,
                      "status": "PAUSED"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.location").value("Bengaluru"))
            .andExpect(jsonPath("$.salaryMax").value(1900000.0))
            .andExpect(jsonPath("$.status").value("PAUSED"));

        mockMvc.perform(delete("/api/v1/jobs/" + jobId))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/jobs/" + jobId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Job not found with id: " + jobId));
    }

    @Test
    void nonRecruiterOwnerIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "QA Engineer",
                      "category": "Engineering",
                      "type": "Full-time",
                      "location": "Hyderabad",
                      "salaryMin": 700000,
                      "salaryMax": 950000,
                      "description": "Test distributed systems.",
                      "skills": ["Selenium", "Java"],
                      "experienceRequired": 2,
                      "postedBy": 202,
                      "status": "OPEN"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("postedBy must reference a recruiter profile"));
    }

    @Test
    void searchFiltersAndTitleLookupWork() throws Exception {
        mockMvc.perform(post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Product Designer",
                      "category": "Design",
                      "type": "Contract",
                      "location": "Remote",
                      "salaryMin": 600000,
                      "salaryMax": 800000,
                      "description": "Design candidate journeys.",
                      "skills": ["Figma", "UX Research"],
                      "experienceRequired": 3,
                      "postedBy": 101,
                      "status": "OPEN"
                    }
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Backend Intern",
                      "category": "Engineering",
                      "type": "Internship",
                      "location": "Remote",
                      "salaryMin": 200000,
                      "salaryMax": 300000,
                      "description": "Support backend feature work.",
                      "skills": ["Java", "Git"],
                      "experienceRequired": 0,
                      "postedBy": 303,
                      "status": "DRAFT"
                    }
                    """))
            .andExpect(status().isCreated());

        String titleLookupResponse = mockMvc.perform(get("/api/v1/jobs/title/Product Designer"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.category").value("Design"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode titleLookupJob = objectMapper.readTree(titleLookupResponse);
        mockMvc.perform(get("/api/v1/jobs")
                .param("location", "remote")
                .param("salaryMax", "850000")
                .param("experienceRequired", "3")
                .param("status", "OPEN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].jobId").value(titleLookupJob.get("jobId").asInt()));
    }

    @TestConfiguration
    static class TestClientConfig {

        @Bean
        @Primary
        RecruiterDirectoryClient recruiterDirectoryClient() {
            return profileId -> {
                if (profileId == 101) {
                    return new RecruiterProfileSnapshot(101, "RECRUITER", "Priya Verma", "priya@hireco.com");
                }
                if (profileId == 303) {
                    return new RecruiterProfileSnapshot(303, "RECRUITER", "Karan Shah", "karan@startup.com");
                }
                if (profileId == 202) {
                    return new RecruiterProfileSnapshot(202, "CANDIDATE", "Aman Sharma", "aman@example.com");
                }
                throw new ApiException(HttpStatus.BAD_REQUEST, "Recruiter profile not found with id: " + profileId);
            };
        }
    }
}
