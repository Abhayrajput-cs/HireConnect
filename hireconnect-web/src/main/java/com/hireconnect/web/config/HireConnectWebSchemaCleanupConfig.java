package com.hireconnect.web.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class HireConnectWebSchemaCleanupConfig {

    @Bean
    ApplicationRunner hireConnectWebSchemaCleanupRunner(JdbcTemplate jdbcTemplate) {
        return args -> {
            jdbcTemplate.execute("DROP TABLE IF EXISTS invoice_records");
            jdbcTemplate.execute("DROP TABLE IF EXISTS recruiter_subscriptions");
            jdbcTemplate.execute("DROP TABLE IF EXISTS subscription_plans");
        };
    }
}
