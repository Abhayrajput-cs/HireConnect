package com.hireconnect.notification.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.hireconnect.notification.client.ProfileServiceClient;
import com.hireconnect.notification.client.ProfileSnapshot;
import com.hireconnect.notification.config.MailProperties;
import com.hireconnect.notification.domain.ApplicationMetric;
import com.hireconnect.notification.domain.Notification;
import com.hireconnect.notification.dto.NotificationEvent;
import com.hireconnect.notification.dto.NotificationResponse;
import com.hireconnect.notification.exception.ApiException;
import com.hireconnect.notification.repository.ApplicationMetricRepository;
import com.hireconnect.notification.repository.NotificationRepository;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final ApplicationMetricRepository applicationMetricRepository;
    private final ProfileServiceClient profileServiceClient;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final MailProperties mailProperties;

    public NotificationServiceImpl(
        NotificationRepository notificationRepository,
        ApplicationMetricRepository applicationMetricRepository,
        ProfileServiceClient profileServiceClient,
        ObjectProvider<JavaMailSender> mailSenderProvider,
        MailProperties mailProperties
    ) {
        this.notificationRepository = notificationRepository;
        this.applicationMetricRepository = applicationMetricRepository;
        this.profileServiceClient = profileServiceClient;
        this.mailSenderProvider = mailSenderProvider;
        this.mailProperties = mailProperties;
    }

    @Override
    @CacheEvict(
        cacheNames = {
            "notificationsByUser", "notificationUnreadCount", "analyticsJobViewCount", "analyticsAppCountByJob",
            "analyticsViewToApplyRatio", "analyticsTimeToHire", "analyticsPipelineStats", "analyticsPlatformStats",
            "analyticsTopJobCategories"
        },
        allEntries = true
    )
    public void sendNotification(NotificationEvent event) {
        LocalDateTime occurredAt = event.getOccurredAt() == null ? LocalDateTime.now() : event.getOccurredAt();
        RecipientResolution recipients = resolveRecipients(event);

        for (Integer recipientId : recipients.userIds()) {
            Notification notification = new Notification();
            notification.setUserId(recipientId);
            notification.setType(normalizeValue(event.getNotificationType(), "GENERAL"));
            notification.setMessage(requiredMessage(event.getMessage()));
            notification.setRead(false);
            notification.setCreatedAt(occurredAt);
            notificationRepository.save(notification);
        }

        if (mailProperties.enabled() && StringUtils.hasText(event.getEmailSubject())) {
            for (String email : recipients.emails()) {
                if (!StringUtils.hasText(email)) {
                    continue;
                }
                sendEmailAlert(
                    email,
                    event.getEmailSubject(),
                    StringUtils.hasText(event.getEmailBody()) ? event.getEmailBody() : event.getMessage()
                );
            }
        }

        updateApplicationMetrics(event, occurredAt);
    }

    @Override
    @CacheEvict(cacheNames = {"notificationsByUser", "notificationUnreadCount"}, allEntries = true)
    public void markAsRead(Integer notificationId) {
        Notification notification = getRequiredNotification(notificationId);
        enforceNotificationOwnership(notification);
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @CacheEvict(cacheNames = {"notificationsByUser", "notificationUnreadCount"}, allEntries = true)
    public void markAllRead(Integer userId) {
        enforceUserAccess(userId);
        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false);
        for (Notification notification : notifications) {
            notification.setRead(true);
        }
        notificationRepository.saveAll(notifications);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "notificationsByUser")
    public List<NotificationResponse> getByUser(Integer userId, Boolean isRead) {
        enforceUserAccess(userId);
        List<Notification> notifications = isRead == null
            ? notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
            : notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, isRead);

        return notifications.stream().map(this::toResponse).toList();
    }

    @Override
    @CacheEvict(cacheNames = {"notificationsByUser", "notificationUnreadCount"}, allEntries = true)
    public void deleteNotification(Integer notificationId) {
        Notification notification = getRequiredNotification(notificationId);
        enforceNotificationOwnership(notification);
        notificationRepository.deleteByNotificationId(notificationId);
    }

    @Override
    public void sendEmailAlert(String toEmail, String subject, String body) {
        if (!mailProperties.enabled()) {
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            LOGGER.warn("Mail sender is not available; email notification to {} was skipped", toEmail);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailProperties.from());
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            LOGGER.warn("Failed to send email notification to {}: {}", toEmail, ex.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "notificationUnreadCount", key = "#userId")
    public int getUnreadCount(Integer userId) {
        enforceUserAccess(userId);
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    private RecipientResolution resolveRecipients(NotificationEvent event) {
        Set<Integer> recipientIds = new LinkedHashSet<>();
        Set<String> recipientEmails = new LinkedHashSet<>();
        if (event.getRecipientUserIds() != null) {
            recipientIds.addAll(event.getRecipientUserIds().stream().filter(id -> id != null && id > 0).toList());
        }
        if (event.getRecipientEmails() != null) {
            recipientEmails.addAll(event.getRecipientEmails().stream().filter(StringUtils::hasText).toList());
        }
        if (StringUtils.hasText(event.getBroadcastRole())) {
            profileServiceClient.getProfilesByRole(event.getBroadcastRole()).stream()
                .forEach(profile -> {
                    recipientIds.add(profile.profileId());
                    if (StringUtils.hasText(profile.email())) {
                        recipientEmails.add(profile.email());
                    }
                });
        }
        return new RecipientResolution(List.copyOf(recipientIds), List.copyOf(recipientEmails));
    }

    private void updateApplicationMetrics(NotificationEvent event, LocalDateTime occurredAt) {
        if (event.getApplicationId() == null || event.getJobId() == null || event.getRecruiterId() == null || event.getCandidateId() == null) {
            return;
        }

        ApplicationMetric metric = applicationMetricRepository.findById(event.getApplicationId())
            .orElseGet(ApplicationMetric::new);
        metric.setApplicationId(event.getApplicationId());
        metric.setJobId(event.getJobId());
        metric.setRecruiterId(event.getRecruiterId());
        metric.setCandidateId(event.getCandidateId());
        metric.setAppliedAt(event.getAppliedAt() == null ? LocalDate.from(occurredAt) : event.getAppliedAt());

        String normalizedStatus = normalizeValue(event.getStatus(), null);
        if (normalizedStatus != null) {
            metric.setLastStatus(normalizedStatus);
            switch (normalizedStatus) {
                case "APPLIED" -> metric.setAppliedAt(event.getAppliedAt() == null ? LocalDate.from(occurredAt) : event.getAppliedAt());
                case "SHORTLISTED" -> metric.setShortlistedAt(occurredAt);
                case "INTERVIEW_SCHEDULED" -> {
                    if (metric.getInterviewScheduledAt() == null) {
                        metric.setInterviewScheduledAt(occurredAt);
                    }
                }
                case "OFFERED" -> metric.setOfferedAt(occurredAt);
                case "REJECTED" -> metric.setRejectedAt(occurredAt);
                case "WITHDRAWN" -> metric.setWithdrawnAt(occurredAt);
                default -> {
                }
            }
        }

        String eventType = normalizeValue(event.getEventType(), "");
        if (eventType.startsWith("INTERVIEW_") && metric.getInterviewScheduledAt() == null) {
            metric.setInterviewScheduledAt(occurredAt);
        }

        applicationMetricRepository.save(metric);
    }

    private Notification getRequiredNotification(Integer notificationId) {
        return notificationRepository.findById(notificationId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found with id: " + notificationId));
    }

    private void enforceNotificationOwnership(Notification notification) {
        enforceUserAccess(notification.getUserId());
    }

    private void enforceUserAccess(Integer userId) {
        if ("ADMIN".equals(currentRole())) {
            return;
        }
        ProfileSnapshot currentProfile = profileServiceClient.getProfileByEmailForAuthUser(currentEmail());
        if (!userId.equals(currentProfile.profileId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Users can only manage their own notifications");
        }
    }

    private String currentEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return authentication.getName();
    }

    private String currentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return authentication.getAuthorities().stream()
            .findFirst()
            .map(grantedAuthority -> grantedAuthority.getAuthority().replace("ROLE_", ""))
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Authentication is required"))
            .toUpperCase(Locale.ROOT);
    }

    private String requiredMessage(String message) {
        if (!StringUtils.hasText(message)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "message is required");
        }
        return message.trim();
    }

    private String normalizeValue(String value, String defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return value.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
            notification.getNotificationId(),
            notification.getUserId(),
            notification.getType(),
            notification.getMessage(),
            notification.isRead(),
            notification.getCreatedAt()
        );
    }

    private record RecipientResolution(
        List<Integer> userIds,
        List<String> emails
    ) {
    }
}
