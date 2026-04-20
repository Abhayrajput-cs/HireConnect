package com.hireconnect.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireconnect.auth.repository.AuthRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void authSchemaUsesSingleUsersTable() throws Exception {
        Connection connection = DataSourceUtils.getConnection(jdbcTemplate.getDataSource());

        try {
            DatabaseMetaData metaData = connection.getMetaData();
            List<String> tables;

            try (ResultSet resultSet = metaData.getTables(connection.getCatalog(), null, "%", new String[] {"TABLE"})) {
                tables = collectApplicationTables(resultSet);
            }

            assertThat(tables).containsExactly("users");
        } finally {
            DataSourceUtils.releaseConnection(connection, jdbcTemplate.getDataSource());
        }
    }

    private List<String> collectApplicationTables(ResultSet resultSet) throws Exception {
        List<String> tables = new java.util.ArrayList<>();
        while (resultSet.next()) {
            String schema = resultSet.getString("TABLE_SCHEM");
            String table = resultSet.getString("TABLE_NAME");
            if (schema == null || table == null) {
                continue;
            }

            String normalizedSchema = schema.toLowerCase(Locale.ROOT);
            if (normalizedSchema.equals("information_schema")
                || normalizedSchema.equals("sys")
                || normalizedSchema.equals("mysql")
                || normalizedSchema.equals("performance_schema")) {
                continue;
            }

            tables.add(table.toLowerCase(Locale.ROOT));
        }

        return tables.stream().sorted().collect(Collectors.toList());
    }

    @Test
    void registerCreatesLocalCandidateAndReturnsTokenBundle() throws Exception {
        String responseBody = mockMvc.perform(post("/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "candidate@example.com",
                      "password": "Secure123",
                      "role": "CANDIDATE"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.user.email").value("candidate@example.com"))
            .andExpect(jsonPath("$.user.role").value("CANDIDATE"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode payload = objectMapper.readTree(responseBody);
        assertThat(authRepository.findByEmail("candidate@example.com"))
            .get()
            .satisfies(user -> assertThat(user.getProvider()).isEqualTo("LOCAL"));

        String accessToken = payload.get("accessToken").asText();
        mockMvc.perform(get("/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("candidate@example.com"));
    }

    @Test
    void duplicateRegistrationIsRejected() throws Exception {
        mockMvc.perform(post("/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "recruiter@example.com",
                      "password": "Secure123",
                      "role": "RECRUITER"
                    }
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "recruiter@example.com",
                      "password": "Secure123",
                      "role": "RECRUITER"
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    void loginRefreshValidateAndLogoutFlowWorksWithSingleTableModel() throws Exception {
        mockMvc.perform(post("/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "tester@example.com",
                      "password": "Secure123",
                      "role": "CANDIDATE"
                    }
                    """))
            .andExpect(status().isCreated());

        String loginBody = mockMvc.perform(post("/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "tester@example.com",
                      "password": "Secure123"
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode loginPayload = objectMapper.readTree(loginBody);
        String accessToken = loginPayload.get("accessToken").asText();
        String refreshToken = loginPayload.get("refreshToken").asText();

        mockMvc.perform(post("/auth/validate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "token": "%s"
                    }
                    """.formatted(accessToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.email").value("tester@example.com"));

        String refreshedBody = mockMvc.perform(post("/auth/refresh")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refreshToken": "%s"
                    }
                    """.formatted(refreshToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String rotatedRefreshToken = objectMapper.readTree(refreshedBody).get("refreshToken").asText();

        mockMvc.perform(post("/auth/logout")
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("Logout processed successfully"));

        mockMvc.perform(post("/auth/validate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "token": "%s"
                    }
                    """.formatted(accessToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.message").value("Token is valid"));

        mockMvc.perform(post("/auth/refresh")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refreshToken": "%s"
                    }
                    """.formatted(rotatedRefreshToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.email").value("tester@example.com"));
    }

    @Test
    void meEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/auth/me"))
            .andExpect(status().isUnauthorized());
    }
}
