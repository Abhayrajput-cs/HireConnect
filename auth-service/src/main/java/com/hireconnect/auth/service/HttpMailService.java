package com.hireconnect.auth.service;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.hireconnect.auth.config.MailProperties;

@Service
public class HttpMailService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final MailProperties mailProperties;

    public HttpMailService(MailProperties mailProperties) {
        this.mailProperties = mailProperties;
    }

    public boolean enabled() {
        return "resend".equalsIgnoreCase(mailProperties.provider())
            || "http".equalsIgnoreCase(mailProperties.provider());
    }

    public void send(String toEmail, String subject, String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(mailProperties.apiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = Map.of(
            "from", mailProperties.from(),
            "to", List.of(toEmail),
            "subject", subject,
            "text", text
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
            mailProperties.apiUrl(),
            new HttpEntity<>(payload, headers),
            String.class
        );
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Email API returned " + response.getStatusCode());
        }
    }

    public void assertConfigured() {
        if (!StringUtils.hasText(mailProperties.apiUrl()) || !StringUtils.hasText(mailProperties.apiKey())) {
            throw new IllegalStateException("Email API URL or key is not configured");
        }
        if (!StringUtils.hasText(mailProperties.from())) {
            throw new IllegalStateException("Email API from address is not configured");
        }
    }
}
