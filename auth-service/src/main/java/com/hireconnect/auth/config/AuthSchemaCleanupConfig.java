package com.hireconnect.auth.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class AuthSchemaCleanupConfig {

    @Bean
    ApplicationRunner authSchemaCleanupRunner(JdbcTemplate jdbcTemplate) {
        return args -> {
            jdbcTemplate.execute("DROP TABLE IF EXISTS refresh_token_sessions");
            jdbcTemplate.execute("DROP TABLE IF EXISTS revoked_tokens");
            jdbcTemplate.execute("DROP TABLE IF EXISTS user_credentials");
        };
    }
}
