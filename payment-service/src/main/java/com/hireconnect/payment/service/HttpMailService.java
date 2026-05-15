package com.hireconnect.payment.service;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.hireconnect.payment.config.MailProperties;

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

    public void send(String toEmail, String subject, String text, String html, PaymentReceiptAttachment attachment) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(mailProperties.apiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("from", resolveFromAddress());
        payload.put("to", List.of(toEmail));
        payload.put("subject", subject);
        payload.put("text", text);
        payload.put("html", html);
        if (StringUtils.hasText(mailProperties.replyTo())) {
            payload.put("reply_to", mailProperties.replyTo());
        }
        if (attachment != null && attachment.content() != null && attachment.content().length > 0) {
            Map<String, Object> file = new LinkedHashMap<>();
            file.put("filename", attachment.filename());
            file.put("content", Base64.getEncoder().encodeToString(attachment.content()));
            payload.put("attachments", List.of(file));
        }

        ResponseEntity<String> response = restTemplate.postForEntity(
            mailProperties.apiUrl(),
            new HttpEntity<>(payload, headers),
            String.class
        );
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Email API returned " + response.getStatusCode());
        }
    }

    public boolean configured() {
        return StringUtils.hasText(mailProperties.apiUrl())
            && StringUtils.hasText(mailProperties.apiKey())
            && StringUtils.hasText(resolveFromAddress());
    }

    private String resolveFromAddress() {
        return StringUtils.hasText(mailProperties.from()) ? mailProperties.from() : "no-reply@hireconnect.local";
    }
}
