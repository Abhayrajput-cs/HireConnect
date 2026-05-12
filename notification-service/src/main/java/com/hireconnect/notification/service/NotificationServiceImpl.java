package com.hireconnect.notification.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.hireconnect.notification.client.ApplicationServiceClient;
import com.hireconnect.notification.client.ApplicationSnapshot;
import com.hireconnect.notification.client.JobServiceClient;
import com.hireconnect.notification.client.JobSnapshot;
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

import jakarta.mail.internet.MimeMessage;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final ApplicationMetricRepository applicationMetricRepository;
    private final ApplicationServiceClient applicationServiceClient;
    private final ProfileServiceClient profileServiceClient;
    private final JobServiceClient jobServiceClient;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final MailProperties mailProperties;
    private final OfferLetterPdfService offerLetterPdfService;

    public NotificationServiceImpl(
        NotificationRepository notificationRepository,
        ApplicationMetricRepository applicationMetricRepository,
        ApplicationServiceClient applicationServiceClient,
        ProfileServiceClient profileServiceClient,
        JobServiceClient jobServiceClient,
        ObjectProvider<JavaMailSender> mailSenderProvider,
        MailProperties mailProperties,
        OfferLetterPdfService offerLetterPdfService
    ) {
        this.notificationRepository = notificationRepository;
        this.applicationMetricRepository = applicationMetricRepository;
        this.applicationServiceClient = applicationServiceClient;
        this.profileServiceClient = profileServiceClient;
        this.jobServiceClient = jobServiceClient;
        this.mailSenderProvider = mailSenderProvider;
        this.mailProperties = mailProperties;
        this.offerLetterPdfService = offerLetterPdfService;
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
            OfferLetterAttachment offerLetter = buildOfferLetterIfNeeded(event);
            if (isOfferEvent(event) && offerLetter == null) {
                LOGGER.warn("Offer email for application {} was skipped because the PDF attachment could not be generated", event.getApplicationId());
                updateApplicationMetrics(event, occurredAt);
                return;
            }
            for (String email : recipients.emails()) {
                if (!StringUtils.hasText(email)) {
                    continue;
                }
                sendEmailAlert(
                    email,
                    event.getEmailSubject(),
                    StringUtils.hasText(event.getEmailBody()) ? event.getEmailBody() : event.getMessage(),
                    offerLetter
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
        sendEmailAlert(toEmail, subject, body, null);
    }

    @Override
    @Transactional(readOnly = true)
    public OfferLetterAttachment buildOfferLetterForApplication(Integer applicationId) {
        ApplicationSnapshot application = applicationServiceClient.getApplication(applicationId);
        if (application == null || application.applicationId() == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Application not found with id: " + applicationId);
        }

        String status = normalizeValue(application.status(), "");
        if (!"OFFERED".equals(status) && !"OFFER_ACCEPTED".equals(status)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Offer letter is available only for offered applications");
        }

        ProfileSnapshot candidate = profileServiceClient.getProfileById(application.candidateId());
        JobSnapshot job = jobServiceClient.getJob(application.jobId());
        if (candidate == null || candidate.profileId() == null || job == null || job.jobId() == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Offer letter details are unavailable right now");
        }
        enforceOfferLetterAccess(application, job);

        ProfileSnapshot recruiter = profileServiceClient.getProfileById(job.postedBy());
        return offerLetterPdfService.build(candidate, recruiter, job);
    }

    private void sendEmailAlert(String toEmail, String subject, String body, OfferLetterAttachment attachment) {
        if (!mailProperties.enabled()) {
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            LOGGER.warn("Mail sender is not available; email notification to {} was skipped", toEmail);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailProperties.from());
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(buildPlainTextEmail(body, attachment), buildHtmlEmail(subject, body, attachment));
            if (attachment != null && attachment.content() != null && attachment.content().length > 0) {
                helper.addAttachment(
                    attachment.filename(),
                    new ByteArrayResource(attachment.content()),
                    "application/pdf"
                );
            }
            mailSender.send(message);
        } catch (Exception ex) {
            LOGGER.warn("Failed to send email notification to {}: {}", toEmail, ex.getMessage());
        }
    }

    private OfferLetterAttachment buildOfferLetterIfNeeded(NotificationEvent event) {
        if (!isOfferEvent(event)) {
            return null;
        }
        if (event.getCandidateId() == null || event.getRecruiterId() == null || event.getJobId() == null) {
            LOGGER.warn("Offer letter attachment skipped because offer event is missing candidate, recruiter, or job id");
            return null;
        }
        try {
            ProfileSnapshot candidate = profileServiceClient.getProfileById(event.getCandidateId());
            ProfileSnapshot recruiter = profileServiceClient.getProfileById(event.getRecruiterId());
            JobSnapshot job = jobServiceClient.getJob(event.getJobId());
            if (candidate == null || job == null) {
                LOGGER.warn("Offer letter attachment skipped because candidate or job details were unavailable");
                return null;
            }
            return offerLetterPdfService.build(candidate, recruiter, job);
        } catch (Exception ex) {
            LOGGER.warn("Offer letter attachment could not be generated for application {}: {}", event.getApplicationId(), ex.getMessage());
            return null;
        }
    }

    private boolean isOfferEvent(NotificationEvent event) {
        String eventType = normalizeValue(event.getEventType(), "");
        String status = normalizeValue(event.getStatus(), "");
        return "APPLICATION_OFFERED".equals(eventType) || "OFFERED".equals(status);
    }

    private String buildPlainTextEmail(String body, OfferLetterAttachment attachment) {
        String attachmentNote = attachment == null ? "" : "\nYour offer letter PDF is attached to this email.\n";
        return """
            HireConnect

            %s
            %s

            Open your HireConnect workspace for full details.
            """.formatted(body == null ? "" : body, attachmentNote);
    }

    private String buildHtmlEmail(String subject, String body, OfferLetterAttachment attachment) {
        String safeSubject = escapeHtml(subject == null ? "HireConnect update" : subject);
        String safeBody = escapeHtml(body == null ? "" : body).replace("\n", "<br>");
        String attachmentNote = attachment == null ? "" : """
                            <div style="margin:0 0 24px;padding:14px 16px;border-radius:12px;background:#eef8ff;border:1px solid #bfe7ff;color:#075985;font-weight:700;">
                              Your offer letter PDF is attached to this email.
                            </div>
            """;
        return """
            <!doctype html>
            <html>
              <body style="margin:0;padding:0;background:#f4f7fb;font-family:Arial,Helvetica,sans-serif;color:#102033;">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f4f7fb;padding:28px 0;">
                  <tr>
                    <td align="center">
                      <table role="presentation" width="620" cellspacing="0" cellpadding="0" style="width:620px;max-width:92%%;background:#ffffff;border:1px solid #dce6f2;border-radius:18px;overflow:hidden;box-shadow:0 18px 45px rgba(16,32,51,.08);">
                        <tr>
                          <td style="background:#06172b;padding:26px 30px;color:#ffffff;">
                            <div style="font-size:12px;letter-spacing:2px;text-transform:uppercase;color:#23b7ff;font-weight:700;">HireConnect workspace</div>
                            <h1 style="margin:10px 0 0;font-size:26px;line-height:1.25;">%s</h1>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:30px;">
                            <p style="margin:0 0 24px;font-size:16px;line-height:1.65;color:#24364b;">%s</p>
                            %s
                            <a href="http://localhost:4200" style="display:inline-block;background:#0f8cff;color:#ffffff;text-decoration:none;font-weight:700;padding:13px 18px;border-radius:10px;">Open HireConnect</a>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:18px 30px;background:#f8fbff;color:#6b7b91;font-size:12px;line-height:1.5;">
                            This automated email was sent by HireConnect. Keep your workspace open for notifications, interview updates, and application decisions.
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>
              </body>
            </html>
            """.formatted(safeSubject, safeBody, attachmentNote);
    }

    private String escapeHtml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
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

    private void enforceOfferLetterAccess(ApplicationSnapshot application, JobSnapshot job) {
        String role = currentRole();
        if ("ADMIN".equals(role)) {
            return;
        }

        ProfileSnapshot currentProfile = profileServiceClient.getProfileByEmailForAuthUser(currentEmail());
        if ("CANDIDATE".equals(role) && Objects.equals(application.candidateId(), currentProfile.profileId())) {
            return;
        }
        if ("RECRUITER".equals(role) && Objects.equals(job.postedBy(), currentProfile.profileId())) {
            return;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "Users can only download offer letters for their own applications");
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
