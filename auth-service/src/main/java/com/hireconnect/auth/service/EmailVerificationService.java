package com.hireconnect.auth.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.hireconnect.auth.config.AuthVerificationProperties;
import com.hireconnect.auth.config.MailProperties;
import com.hireconnect.auth.domain.UserCredential;
import com.hireconnect.auth.exception.ApiException;

@Service
public class EmailVerificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final MailProperties mailProperties;
    private final AuthVerificationProperties verificationProperties;

    public EmailVerificationService(
        ObjectProvider<JavaMailSender> mailSenderProvider,
        MailProperties mailProperties,
        AuthVerificationProperties verificationProperties
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailProperties = mailProperties;
        this.verificationProperties = verificationProperties;
    }

    public void prepareVerification(UserCredential user) {
        if (!verificationProperties.enabled()) {
            user.setEmailVerified(true);
            user.setEmailVerificationCode(null);
            user.setEmailVerificationExpiresAt(null);
            return;
        }
        user.setEmailVerified(false);
        user.setEmailVerificationCode(generateCode());
        user.setEmailVerificationExpiresAt(LocalDateTime.now().plusMinutes(verificationProperties.otpTtlMinutes()));
    }

    public void sendVerification(UserCredential user) {
        if (!verificationProperties.enabled() || user.isEmailVerified()) {
            return;
        }
        if (!mailProperties.enabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Email verification is enabled, but SMTP mail sending is disabled.");
        }
        assertSmtpConfigured();

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Email service is unavailable. Please try again in a moment.");
        }

        try {
            mailSender.send(buildMessage(
                user.getEmail(),
                "Verify your HireConnect email",
                """
                    Welcome to HireConnect.

                    Your verification code is: %s

                    This code expires in %d minutes.
                    """.formatted(user.getEmailVerificationCode(), verificationProperties.otpTtlMinutes())
            ));
        } catch (Exception ex) {
            LOGGER.warn("Failed to send verification email to {}", user.getEmail(), ex);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "We could not send the verification email. Please check SMTP settings and try again.");
        }
    }

    public void preparePasswordReset(UserCredential user) {
        user.setPasswordResetCode(generateCode());
        user.setPasswordResetExpiresAt(LocalDateTime.now().plusMinutes(verificationProperties.otpTtlMinutes()));
    }

    public void sendPasswordReset(UserCredential user) {
        if (!mailProperties.enabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Password reset email cannot be sent because SMTP mail sending is disabled.");
        }
        assertSmtpConfigured();

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Email service is unavailable. Please try again in a moment.");
        }

        try {
            mailSender.send(buildMessage(
                user.getEmail(),
                "Reset your HireConnect password",
                """
                    We received a request to reset your HireConnect password.

                    Your reset code is: %s

                    This code expires in %d minutes. If you did not request this, ignore this email.
                    """.formatted(user.getPasswordResetCode(), verificationProperties.otpTtlMinutes())
            ));
        } catch (Exception ex) {
            LOGGER.warn("Failed to send password reset email to {}", user.getEmail(), ex);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "We could not send the password reset email. Please check SMTP settings and try again.");
        }
    }

    private SimpleMailMessage buildMessage(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(resolveFromAddress());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        return message;
    }

    private String resolveFromAddress() {
        return StringUtils.hasText(mailProperties.from()) ? mailProperties.from() : mailProperties.username();
    }

    private void assertSmtpConfigured() {
        if (!StringUtils.hasText(mailProperties.username())
            || !StringUtils.hasText(mailProperties.password())
            || "your-email@gmail.com".equalsIgnoreCase(mailProperties.username())
            || "your-gmail-app-password".equals(mailProperties.password())) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SMTP credentials are not configured for OTP email delivery.");
        }

        if (mailProperties.password().contains(" ")) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SMTP app password should not contain spaces. Remove spaces from MAIL_PASSWORD and restart auth-service.");
        }

        if (!StringUtils.hasText(resolveFromAddress())) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SMTP from address is not configured.");
        }
    }

    private String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
