package com.hireconnect.profile;

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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void candidateProfileCrudAndLookupsWork() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/profiles/candidates")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullName": "Aman Sharma",
                      "email": "aman@example.com",
                      "mobile": 9876543210,
                      "dob": "1999-03-15",
                      "gender": "MALE",
                      "skills": ["Java", "Spring Boot", "MySQL"],
                      "experience": 3,
                      "resumeUrl": "https://files.example.com/resume/aman.pdf",
                      "addresses": [
                        {
                          "houseNo": "12A",
                          "street": "MG Road",
                          "city": "Pune",
                          "state": "Maharashtra",
                          "pincode": 411001
                        }
                      ]
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("CANDIDATE"))
            .andExpect(jsonPath("$.skills", hasSize(3)))
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode createdCandidate = objectMapper.readTree(createResponse);
        int profileId = createdCandidate.get("profileId").asInt();

        mockMvc.perform(get("/api/v1/profiles/" + profileId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("aman@example.com"))
            .andExpect(jsonPath("$.addresses[0].city").value("Pune"));

        mockMvc.perform(get("/api/v1/profiles/email/aman@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.profileId").value(profileId));

        mockMvc.perform(get("/api/v1/profiles/mobile/9876543210"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fullName").value("Aman Sharma"));

        mockMvc.perform(put("/api/v1/profiles/" + profileId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullName": "Aman K Sharma",
                      "experience": 4,
                      "skills": ["Java", "Spring Boot", "AWS"],
                      "addresses": [
                        {
                          "houseNo": "44B",
                          "street": "Baner Road",
                          "city": "Pune",
                          "state": "Maharashtra",
                          "pincode": 411045
                        }
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fullName").value("Aman K Sharma"))
            .andExpect(jsonPath("$.experience").value(4))
            .andExpect(jsonPath("$.skills[2]").value("AWS"))
            .andExpect(jsonPath("$.addresses[0].street").value("Baner Road"));
    }

    @Test
    void recruiterProfileCreationFilteringAndDeletionWork() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/profiles/recruiters")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullName": "Priya Verma",
                      "email": "priya@hireco.com",
                      "mobile": 9988776655,
                      "companyName": "HireCo",
                      "companySize": "201-500",
                      "industry": "Software",
                      "website": "https://hireco.example.com",
                      "addresses": [
                        {
                          "houseNo": "9",
                          "street": "Cyber City",
                          "city": "Gurgaon",
                          "state": "Haryana",
                          "pincode": 122002
                        }
                      ]
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("RECRUITER"))
            .andExpect(jsonPath("$.companyName").value("HireCo"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        int profileId = objectMapper.readTree(createResponse).get("profileId").asInt();

        mockMvc.perform(get("/api/v1/profiles/role/RECRUITER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].email").value("priya@hireco.com"));

        mockMvc.perform(get("/api/v1/profiles").param("role", "RECRUITER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].companySize").value("201-500"));

        mockMvc.perform(delete("/api/v1/profiles/" + profileId))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/profiles/" + profileId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Profile not found with id: " + profileId));
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/profiles/candidates")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullName": "First Candidate",
                      "email": "duplicate@example.com",
                      "mobile": 9123456780,
                      "skills": ["Java"],
                      "experience": 2,
                      "resumeUrl": "https://files.example.com/resume/first.pdf"
                    }
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/profiles/recruiters")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullName": "Duplicate Recruiter",
                      "email": "duplicate@example.com",
                      "mobile": 9234567890,
                      "companyName": "Duplicate Inc",
                      "industry": "Consulting"
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Email is already associated with another profile"));
    }
}
