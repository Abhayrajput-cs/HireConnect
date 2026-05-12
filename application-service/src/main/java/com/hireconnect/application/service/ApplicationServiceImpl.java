package com.hireconnect.application.service;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.hireconnect.application.client.CandidateDirectoryClient;
import com.hireconnect.application.client.CandidateProfileSnapshot;
import com.hireconnect.application.client.InterviewCatalogClient;
import com.hireconnect.application.client.JobCatalogClient;
import com.hireconnect.application.client.JobSnapshot;
import com.hireconnect.application.client.SubscriptionAccessClient;
import com.hireconnect.application.domain.Application;
import com.hireconnect.application.dto.ApplicationRequest;
import com.hireconnect.application.dto.ApplicationResponse;
import com.hireconnect.application.exception.ApiException;
import com.hireconnect.application.messaging.NotificationEvent;
import com.hireconnect.application.messaging.NotificationEventPublisher;
import com.hireconnect.application.repository.ApplicationRepository;

@Service
@Transactional
public class ApplicationServiceImpl implements ApplicationService {

    private static final String APPLIED = "APPLIED";
    private static final String SHORTLISTED = "SHORTLISTED";
    private static final String INTERVIEW_SCHEDULED = "INTERVIEW_SCHEDULED";
    private static final String OFFERED = "OFFERED";
    private static final String OFFER_ACCEPTED = "OFFER_ACCEPTED";
    private static final String OFFER_DECLINED = "OFFER_DECLINED";
    private static final String REJECTED = "REJECTED";
    private static final String WITHDRAWN = "WITHDRAWN";
    private static final int FREE_CANDIDATE_APPLICATION_LIMIT = 3;
    private static final Set<String> VALID_STATUSES = Set.of(
        APPLIED,
        SHORTLISTED,
        INTERVIEW_SCHEDULED,
        OFFERED,
        OFFER_ACCEPTED,
        OFFER_DECLINED,
        REJECTED,
        WITHDRAWN
    );
    private static final Map<ApplicationStatus, Set<ApplicationStatus>> RECRUITER_TRANSITIONS = buildTransitions();
    private static final Set<ApplicationStatus> WITHDRAWABLE_STATUSES = Set.of(
        ApplicationStatus.APPLIED,
        ApplicationStatus.SHORTLISTED,
        ApplicationStatus.INTERVIEW_SCHEDULED
    );

    private final ApplicationRepository applicationRepository;
    private final CandidateDirectoryClient candidateDirectoryClient;
    private final JobCatalogClient jobCatalogClient;
    private final InterviewCatalogClient interviewCatalogClient;
    private final NotificationEventPublisher notificationEventPublisher;
    private final SubscriptionAccessClient subscriptionAccessClient;

    public ApplicationServiceImpl(
        ApplicationRepository applicationRepository,
        CandidateDirectoryClient candidateDirectoryClient,
        JobCatalogClient jobCatalogClient,
        InterviewCatalogClient interviewCatalogClient,
        NotificationEventPublisher notificationEventPublisher,
        SubscriptionAccessClient subscriptionAccessClient
    ) {
        this.applicationRepository = applicationRepository;
        this.candidateDirectoryClient = candidateDirectoryClient;
        this.jobCatalogClient = jobCatalogClient;
        this.interviewCatalogClient = interviewCatalogClient;
        this.notificationEventPublisher = notificationEventPublisher;
        this.subscriptionAccessClient = subscriptionAccessClient;
    }

    @Override
    public ApplicationResponse submitApplication(ApplicationRequest request) {
        CandidateProfileSnapshot candidateProfile = candidateDirectoryClient.getCandidateProfile(request.candidateId());
        validateCandidate(candidateProfile, request.candidateId());
        ensureAuthenticatedCandidateOwnsProfile(candidateProfile);

        JobSnapshot job = jobCatalogClient.getJob(request.jobId());
        validateJob(job, request.jobId());

        applicationRepository.findFirstByJobIdAndCandidateId(request.jobId(), request.candidateId())
            .ifPresent(existing -> {
                throw new ApiException(HttpStatus.CONFLICT, "Candidate has already applied to this job");
            });
        enforceFreeCandidateApplicationLimit(request.candidateId());

        Application application = new Application();
        application.setJobId(request.jobId());
        application.setCandidateId(request.candidateId());
        application.setAppliedAt(LocalDate.now());
        application.setStatus(APPLIED);
        application.setCoverLetter(request.coverLetter());
        application.setResumeUrl(resolveResumeUrl(request.resumeUrl(), candidateProfile.resumeUrl()));
        Application saved = applicationRepository.save(application);
        CandidateProfileSnapshot recruiter = loadProfileQuietly(job.postedBy());
        publishApplicationEvent(
            "APPLICATION_SUBMITTED",
            "APPLICATION",
            "New application received for " + job.title(),
            recruiter == null ? List.of() : List.of(job.postedBy()),
            recruiter == null ? List.of() : List.of(recruiter.email()),
            "New application received",
            candidateProfile.email() + " applied for " + job.title(),
            saved,
            job,
            candidateProfile
        );
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getByCandidate(Integer candidateId) {
        enforceCandidateOwnershipIfNeeded(candidateId);
        return applicationRepository.findByCandidateIdOrderByAppliedAtDesc(candidateId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getByJob(Integer jobId) {
        validateJob(jobCatalogClient.getJob(jobId), jobId);
        return applicationRepository.findByJobIdOrderByAppliedAtDesc(jobId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getByStatus(String status) {
        String normalizedStatus = normalizeStatus(status);
        return applicationRepository.findByStatusOrderByAppliedAtDesc(normalizedStatus).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getByAppliedDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "appliedFrom must be on or before appliedTo");
        }
        return applicationRepository.findByAppliedAtBetweenOrderByAppliedAtDesc(startDate, endDate).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getById(Integer applicationId) {
        Application application = getRequiredApplication(applicationId);
        enforceReadAccessIfCandidate(application);
        return toResponse(application);
    }

    @Override
    public ApplicationResponse updateStatus(Integer applicationId, String status) {
        Application application = getRequiredApplication(applicationId);
        ApplicationStatus currentStatus = ApplicationStatus.from(application.getStatus());
        ApplicationStatus nextStatus = ApplicationStatus.from(normalizeStatus(status));

        if (currentStatus == ApplicationStatus.WITHDRAWN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Withdrawn applications cannot move to a new status");
        }
        if (!RECRUITER_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(nextStatus)) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "Invalid application status transition from " + currentStatus.name() + " to " + nextStatus.name()
            );
        }
        if (currentStatus == ApplicationStatus.INTERVIEW_SCHEDULED
            && (nextStatus == ApplicationStatus.OFFERED || nextStatus == ApplicationStatus.REJECTED)) {
            ensureInterviewCompleted(application.getApplicationId(), nextStatus);
        }

        application.setStatus(nextStatus.name());
        Application saved = applicationRepository.save(application);
        publishStatusNotification(saved, nextStatus);
        return toResponse(saved);
    }

    @Override
    public ApplicationResponse withdrawApplication(Integer applicationId) {
        Application application = getRequiredApplication(applicationId);
        CandidateProfileSnapshot candidateProfile = candidateDirectoryClient.getCandidateProfile(application.getCandidateId());
        validateCandidate(candidateProfile, application.getCandidateId());
        ensureAuthenticatedCandidateOwnsProfile(candidateProfile);

        ApplicationStatus currentStatus = ApplicationStatus.from(application.getStatus());
        if (!WITHDRAWABLE_STATUSES.contains(currentStatus)) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "Applications in status " + currentStatus.name() + " cannot be withdrawn"
            );
        }

        application.setStatus(WITHDRAWN);
        return toResponse(applicationRepository.save(application));
    }

    @Override
    public ApplicationResponse acceptOffer(Integer applicationId) {
        return respondToOffer(applicationId, ApplicationStatus.OFFER_ACCEPTED);
    }

    @Override
    public ApplicationResponse declineOffer(Integer applicationId) {
        return respondToOffer(applicationId, ApplicationStatus.OFFER_DECLINED);
    }

    @Override
    @Transactional(readOnly = true)
    public int countByJob(Integer jobId) {
        validateJob(jobCatalogClient.getJob(jobId), jobId);
        return applicationRepository.countByJobId(jobId);
    }

    private Application getRequiredApplication(Integer applicationId) {
        return applicationRepository.findById(applicationId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found with id: " + applicationId));
    }

    private void validateCandidate(CandidateProfileSnapshot candidateProfile, Integer candidateId) {
        if (candidateProfile.profileId() == null || !candidateId.equals(candidateProfile.profileId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Candidate profile not found with id: " + candidateId);
        }
        if (!"CANDIDATE".equalsIgnoreCase(candidateProfile.role())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "candidateId must reference a candidate profile");
        }
    }

    private void validateJob(JobSnapshot job, Integer jobId) {
        if (job.jobId() == null || !jobId.equals(job.jobId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Job not found with id: " + jobId);
        }
        if (!"OPEN".equalsIgnoreCase(job.status())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Applications are allowed only for OPEN jobs");
        }
    }

    private void enforceCandidateOwnershipIfNeeded(Integer candidateId) {
        if (!hasAuthenticatedUser()) {
            return;
        }
        if (!currentRole().equals("CANDIDATE")) {
            return;
        }
        CandidateProfileSnapshot authenticatedCandidate = candidateDirectoryClient.getCandidateProfileByEmail(currentEmail());
        if (!candidateId.equals(authenticatedCandidate.profileId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Candidates can only view their own applications");
        }
    }

    private void enforceReadAccessIfCandidate(Application application) {
        if (!hasAuthenticatedUser()) {
            return;
        }
        if (!currentRole().equals("CANDIDATE")) {
            return;
        }
        CandidateProfileSnapshot authenticatedCandidate = candidateDirectoryClient.getCandidateProfileByEmail(currentEmail());
        if (!application.getCandidateId().equals(authenticatedCandidate.profileId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Candidates can only view their own applications");
        }
    }

    private void ensureAuthenticatedCandidateOwnsProfile(CandidateProfileSnapshot candidateProfile) {
        if (!hasAuthenticatedUser()) {
            return;
        }
        if (!candidateProfile.email().equalsIgnoreCase(currentEmail())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Candidates can only act on their own profile");
        }
    }

    private boolean hasAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && StringUtils.hasText(authentication.getName()) && !"anonymousUser".equals(authentication.getName());
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
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Authentication is required"));
    }

    private String resolveResumeUrl(String requestResumeUrl, String candidateResumeUrl) {
        if (StringUtils.hasText(requestResumeUrl)) {
            return requestResumeUrl;
        }
        if (StringUtils.hasText(candidateResumeUrl)) {
            return candidateResumeUrl;
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "A resumeUrl is required for job applications");
    }

    private String normalizeStatus(String status) {
        String normalizedStatus = status == null ? null : status.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (!VALID_STATUSES.contains(normalizedStatus)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported application status: " + status);
        }
        return normalizedStatus;
    }

    private ApplicationResponse toResponse(Application application) {
        return new ApplicationResponse(
            application.getApplicationId(),
            application.getJobId(),
            application.getCandidateId(),
            application.getAppliedAt(),
            application.getStatus(),
            application.getCoverLetter(),
            application.getResumeUrl()
        );
    }

    private ApplicationResponse respondToOffer(Integer applicationId, ApplicationStatus nextStatus) {
        Application application = getRequiredApplication(applicationId);
        CandidateProfileSnapshot candidateProfile = candidateDirectoryClient.getCandidateProfile(application.getCandidateId());
        validateCandidate(candidateProfile, application.getCandidateId());
        ensureAuthenticatedCandidateOwnsProfile(candidateProfile);

        ApplicationStatus currentStatus = ApplicationStatus.from(application.getStatus());
        if (currentStatus != ApplicationStatus.OFFERED) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "Only offered applications can be accepted or declined"
            );
        }

        application.setStatus(nextStatus.name());
        Application saved = applicationRepository.save(application);
        publishStatusNotification(saved, nextStatus);
        return toResponse(saved);
    }

    private void enforceFreeCandidateApplicationLimit(Integer candidateProfileId) {
        if (subscriptionAccessClient.hasPremiumAccess(candidateProfileId)) {
            return;
        }
        int applications = applicationRepository.countByCandidateId(candidateProfileId);
        if (applications >= FREE_CANDIDATE_APPLICATION_LIMIT) {
            throw new ApiException(
                HttpStatus.PAYMENT_REQUIRED,
                "Free candidates can apply to up to 3 jobs. Upgrade to Candidate Premium for unlimited applications."
            );
        }
    }

    private void ensureInterviewCompleted(Integer applicationId, ApplicationStatus nextStatus) {
        boolean confirmed = interviewCatalogClient.getByApplication(applicationId).stream()
            .anyMatch(interview -> "CONFIRMED".equalsIgnoreCase(interview.status()));
        if (!confirmed) {
            String action = nextStatus == ApplicationStatus.OFFERED ? "offer" : "reject after interview";
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "Recruiter can " + action + " only after the candidate confirms the interview"
            );
        }
    }

    private void publishStatusNotification(Application application, ApplicationStatus status) {
        JobSnapshot job = jobCatalogClient.getJob(application.getJobId());
        CandidateProfileSnapshot candidate = candidateDirectoryClient.getCandidateProfile(application.getCandidateId());
        String title = job.title() == null ? "the role" : job.title();
        String message = switch (status) {
            case SHORTLISTED -> "You were shortlisted for " + title + ".";
            case INTERVIEW_SCHEDULED -> "Interview scheduling has started for " + title + ".";
            case OFFERED -> "You received an offer for " + title + ".";
            case REJECTED -> "Your application for " + title + " was rejected.";
            case OFFER_ACCEPTED -> "Candidate accepted the offer for " + title + ".";
            case OFFER_DECLINED -> "Candidate declined the offer for " + title + ".";
            default -> "Application updated for " + title + ".";
        };
        boolean notifyRecruiter = status == ApplicationStatus.OFFER_ACCEPTED || status == ApplicationStatus.OFFER_DECLINED;
        CandidateProfileSnapshot recruiter = notifyRecruiter ? loadProfileQuietly(job.postedBy()) : null;
        publishApplicationEvent(
            "APPLICATION_" + status.name(),
            "APPLICATION",
            message,
            notifyRecruiter ? List.of(job.postedBy()) : List.of(application.getCandidateId()),
            notifyRecruiter && recruiter != null ? List.of(recruiter.email()) : List.of(candidate.email()),
            "Application update: " + title,
            message,
            application,
            job,
            candidate
        );
    }

    private void publishApplicationEvent(
        String eventType,
        String notificationType,
        String message,
        List<Integer> recipientUserIds,
        List<String> recipientEmails,
        String emailSubject,
        String emailBody,
        Application application,
        JobSnapshot job,
        CandidateProfileSnapshot candidate
    ) {
        notificationEventPublisher.publish(new NotificationEvent(
            eventType,
            notificationType,
            message,
            recipientUserIds,
            recipientEmails,
            null,
            emailSubject,
            emailBody,
            application.getApplicationId(),
            application.getJobId(),
            job.postedBy(),
            candidate.profileId(),
            application.getStatus(),
            application.getAppliedAt(),
            java.time.LocalDateTime.now()
        ));
    }

    private CandidateProfileSnapshot loadProfileQuietly(Integer profileId) {
        try {
            return candidateDirectoryClient.getCandidateProfile(profileId);
        } catch (Exception ex) {
            return null;
        }
    }

    private static Map<ApplicationStatus, Set<ApplicationStatus>> buildTransitions() {
        Map<ApplicationStatus, Set<ApplicationStatus>> transitions = new EnumMap<>(ApplicationStatus.class);
        transitions.put(ApplicationStatus.APPLIED, Set.of(ApplicationStatus.SHORTLISTED, ApplicationStatus.REJECTED));
        transitions.put(ApplicationStatus.SHORTLISTED, Set.of(ApplicationStatus.INTERVIEW_SCHEDULED, ApplicationStatus.REJECTED));
        transitions.put(ApplicationStatus.INTERVIEW_SCHEDULED, Set.of(ApplicationStatus.OFFERED, ApplicationStatus.REJECTED));
        transitions.put(ApplicationStatus.OFFERED, Set.of());
        transitions.put(ApplicationStatus.OFFER_ACCEPTED, Set.of());
        transitions.put(ApplicationStatus.OFFER_DECLINED, Set.of());
        transitions.put(ApplicationStatus.REJECTED, Set.of());
        transitions.put(ApplicationStatus.WITHDRAWN, Set.of());
        return transitions;
    }

    private enum ApplicationStatus {
        APPLIED,
        SHORTLISTED,
        INTERVIEW_SCHEDULED,
        OFFERED,
        OFFER_ACCEPTED,
        OFFER_DECLINED,
        REJECTED,
        WITHDRAWN;

        private static ApplicationStatus from(String status) {
            try {
                return ApplicationStatus.valueOf(status);
            } catch (IllegalArgumentException ex) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported application status: " + status);
            }
        }
    }
}
