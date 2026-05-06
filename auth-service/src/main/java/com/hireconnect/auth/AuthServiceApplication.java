package com.hireconnect.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.hireconnect.auth.config.AuthVerificationProperties;
import com.hireconnect.auth.config.JwtProperties;
import com.hireconnect.auth.config.MailProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, MailProperties.class, AuthVerificationProperties.class})
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

}
