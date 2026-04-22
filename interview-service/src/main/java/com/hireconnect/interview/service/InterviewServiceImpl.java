package com.hireconnect.interview.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.hireconnect.interview.client.ApplicationCatalogClient;
import com.hireconnect.interview.client.ApplicationSnapshot;
import com.hireconnect.interview.client.JobCatalogClient;
import com.hireconnect.interview.client.JobSnapshot;
import com.hireconnect.interview.client.ProfileDirectoryClient;
import com.hireconnect.interview.client.ProfileSnapshot;
import com.hireconnect.interview.domain.Interview;
import com.hireconnect.interview.dto.InterviewRescheduleRequest;
import com.hireconnect.interview.dto.InterviewResponse;
import com.hireconnect.interview.dto.InterviewScheduleRequest;
import com.hireconnect.interview.exception.ApiException;
import com.hireconnect.interview.messaging.NotificationEvent;
import com.hireconnect.interview.messaging.NotificationEventPublisher;
import com.hireconnect.interview.repository.InterviewRepository;

@Service
@Transactional
public class InterviewServiceImpl implements InterviewService {

    private static final String RECRUITER = "RECRUITER";
    private static final String CANDIDATE = "CANDIDATE";
    private static final String MODE_ONLINE = "ONLINE";
    private static final String MODE_IN_PERSON = "IN_PERSON";
    private static final String STATUS_SCHEDULED = "SCHEDULED";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_RESCHEDULE_REQUESTED = "RESCHEDULE_REQUESTED";
    private static final String STATUS_RESCHEDULED = "RESCHEDULED";
    private static final String STATUS_CANCELED = "CANCELED";
    private static final Set<String> VALID_MODES = Set.of(MODE_ONLINE, MODE_IN_PERSON);
    private static final Set<String> VALID_STATUSES = Set.of(
        STATUS_SCHEDULED,
        STATUS_CONFIRMED,
        STATUS_RESCHEDULE_REQUESTED,
        STATUS_RESCHEDULED,
        STATUS_CANCELED
    );
    private static final Set<String> CONFIRMABLE_STATUSES = Set.of(STATUS_SCHEDULED, STATUS_RESCHEDULED);
    private static final Set<String> RESCHEDULABLE_STATUSES = Set.of(
        STATUS_SCHEDULED,
        STATUS_CONFIRMED,
        STATUS_RESCHEDULED,
        STATUS_RESCHEDULE_REQUESTED
    );
    private static final Set<String> APPLICATION_SCHEDULABLE_STATUSES = Set.of("SHORTLISTED", "INTERVIEW_SCHEDULED");

    private final InterviewRepository interviewRepository;
    private final ApplicationCatalogClient applicationCatalogClient;
    private final JobCatalogClient jobCatalogClient;
    private final ProfileDirectoryClient profileDirectoryClient;
    private final NotificationEventPublisher notificationEventPublisher;

    public InterviewServiceImpl(
        InterviewRepository interviewRepository,
        ApplicationCatalogClient applicationCatalogClient,
        JobCatalogClient jobCatalogClient,
        ProfileDirectoryClient profileDirectoryClient,
        NotificationEventPublisher notificationEventPublisher
    ) {
        this.interviewRepository = interviewRepository;
        this.applicationCatalogClient = applicationCatalogClient;
        this.jobCatalogClient = jobCatalogClient;
        this.profileDirectoryClient = profileDirectoryClient;
        this.notificationEventPublisher = notificationEventPublisher;
    }

    @Override
    public InterviewResponse scheduleInterview(InterviewScheduleRequest request) {
        ProfileSnapshot recruiter = requireAuthenticatedProfile(RECRUITER);
        ApplicationSnapshot application = getSchedulableApplication(request.applicationId());
        JobSnapshot job = jobCatalogClient.getJob(application.jobId());
        ProfileSnapshot candidate = profileDirectoryClient.getProfileById(application.candidateId());
        validateRecruiterOwnership(recruiter, job);

        Interview interview = new Interview();
        interview.setApplicationId(request.applicationId());
        interview.setScheduledAt(validateFutureDateTime(request.scheduledAt()));
        interview.setMode(normalizeMode(request.mode()));
        interview.setMeetLink(trimToNull(request.meetLink()));
        interview.setLocation(trimToNull(request.location()));
        interview.setStatus(STATUS_SCHEDULED);
        interview.setNotes(trimToNull(request.notes()));
        validateModeSpecificFields(interview.getMode(), interview.getMeetLink(), interview.getLocation());

        Interview savedInterview = interviewRepository.save(interview);
        if ("SHORTLISTED".equalsIgnoreCase(application.status())) {
            applicationCatalogClient.markInterviewScheduled(application.applicationId());
        }
        publishInterviewEvent(
            "INTERVIEW_SCHEDULED",
            List.of(application.candidateId(), recruiter.profileId()),
            List.of(candidate.email(), recruiter.email()),
            "Interview scheduled for application " + application.applicationId(),
            "Interview scheduled",
            "An interview has been scheduled for application " + application.applicationId() + " and job " + job.title(),
            savedInterview,
            application,
            job
        );
        return toResponse(savedInterview);
    }

    @Override
    public String confirmInterview(Integer interviewId) {
        Interview interview = getRequiredInterview(interviewId);
        if (!CONFIRMABLE_STATUSES.contains(interview.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Interview cannot be confirmed from status " + interview.getStatus());
        }

        ApplicationSnapshot application = applicationCatalogClient.getApplication(interview.getApplicationId());
        ProfileSnapshot candidate = requireAuthenticatedProfile(CANDIDATE);
        validateCandidateOwnership(candidate, application);

        interview.setStatus(STATUS_CONFIRMED);
        interviewRepository.save(interview);
        JobSnapshot job = jobCatalogClient.getJob(application.jobId());
        ProfileSnapshot recruiter = profileDirectoryClient.getProfileById(job.postedBy());
        publishInterviewEvent(
            "INTERVIEW_CONFIRMED",
            List.of(job.postedBy()),
            List.of(recruiter.email()),
            "Candidate confirmed interview for application " + application.applicationId(),
            "Interview confirmed",
            "Candidate confirmed the scheduled interview for job " + job.title(),
            interview,
            application,
            job
        );
        return "Interview confirmed successfully";
    }

    @Override
    public InterviewResponse rescheduleInterview(Integer interviewId, InterviewRescheduleRequest request) {
        Interview interview = getRequiredInterview(interviewId);
        if (!RESCHEDULABLE_STATUSES.contains(interview.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Interview cannot be rescheduled from status " + interview.getStatus());
        }

        ApplicationSnapshot application = applicationCatalogClient.getApplication(interview.getApplicationId());
        String actorRole = currentRole();
        List<Integer> recipients;
        List<String> recipientEmails;
        String eventType;
        String message;
        String emailSubject;
        String emailBody;
        JobSnapshot job = jobCatalogClient.getJob(application.jobId());
        if (RECRUITER.equals(actorRole)) {
            ProfileSnapshot recruiter = requireAuthenticatedProfile(RECRUITER);
            validateRecruiterOwnership(recruiter, job);
            interview.setStatus(STATUS_RESCHEDULED);
            recipients = List.of(application.candidateId());
            recipientEmails = List.of(profileDirectoryClient.getProfileById(application.candidateId()).email());
            eventType = "INTERVIEW_RESCHEDULED";
            message = "Interview rescheduled for application " + application.applicationId();
            emailSubject = "Interview rescheduled";
            emailBody = "Your interview for job " + job.title() + " was rescheduled";
        } else if (CANDIDATE.equals(actorRole)) {
            ProfileSnapshot candidate = requireAuthenticatedProfile(CANDIDATE);
            validateCandidateOwnership(candidate, application);
            interview.setStatus(STATUS_RESCHEDULE_REQUESTED);
            recipients = List.of(job.postedBy());
            recipientEmails = List.of(profileDirectoryClient.getProfileById(job.postedBy()).email());
            eventType = "INTERVIEW_RESCHEDULE_REQUESTED";
            message = "Candidate requested interview reschedule for application " + application.applicationId();
            emailSubject = "Interview reschedule requested";
            emailBody = "Candidate requested a new slot for job " + job.title();
        } else {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only candidates or recruiters can reschedule interviews");
        }

        interview.setScheduledAt(validateFutureDateTime(request.scheduledAt()));
        if (request.meetLink() != null) {
            interview.setMeetLink(trimToNull(request.meetLink()));
        }
        if (request.location() != null) {
            interview.setLocation(trimToNull(request.location()));
        }
        if (request.notes() != null) {
            interview.setNotes(trimToNull(request.notes()));
        }
        validateModeSpecificFields(interview.getMode(), interview.getMeetLink(), interview.getLocation());
        Interview savedInterview = interviewRepository.save(interview);
        publishInterviewEvent(eventType, recipients, recipientEmails, message, emailSubject, emailBody, savedInterview, application, job);
        return toResponse(savedInterview);
    }

    @Override
    public void cancelInterview(Integer interviewId) {
        Interview interview = getRequiredInterview(interviewId);
        ApplicationSnapshot application = applicationCatalogClient.getApplication(interview.getApplicationId());
        ProfileSnapshot recruiter = requireAuthenticatedProfile(RECRUITER);
        JobSnapshot job = jobCatalogClient.getJob(application.jobId());
        validateRecruiterOwnership(recruiter, job);
        ProfileSnapshot candidate = profileDirectoryClient.getProfileById(application.candidateId());

        interview.setStatus(STATUS_CANCELED);
        interviewRepository.save(interview);
        publishInterviewEvent(
            "INTERVIEW_CANCELED",
            List.of(application.candidateId(), recruiter.profileId()),
            List.of(candidate.email(), recruiter.email()),
            "Interview canceled for application " + application.applicationId(),
            "Interview canceled",
            "The interview for job " + job.title() + " was canceled",
            interview,
            application,
            job
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewResponse> getByApplication(Integer applicationId) {
        ApplicationSnapshot application = applicationCatalogClient.getApplication(applicationId);
        validateReadAccess(application);
        return interviewRepository.findByApplicationIdOrderByScheduledAtAsc(applicationId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewResponse> getByStatus(String status) {
        requireAuthenticatedProfile(RECRUITER);
        return interviewRepository.findByStatusOrderByScheduledAtAsc(normalizeStatus(status)).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewResponse> getByScheduledRange(LocalDateTime scheduledFrom, LocalDateTime scheduledTo) {
        requireAuthenticatedProfile(RECRUITER);
        if (scheduledFrom.isAfter(scheduledTo)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "scheduledFrom must be on or before scheduledTo");
        }
        return interviewRepository.findByScheduledAtBetweenOrderByScheduledAtAsc(scheduledFrom, scheduledTo).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewResponse getById(Integer interviewId) {
        Interview interview = getRequiredInterview(interviewId);
        ApplicationSnapshot application = applicationCatalogClient.getApplication(interview.getApplicationId());
        validateReadAccess(application);
        return toResponse(interview);
    }

    private ApplicationSnapshot getSchedulableApplication(Integer applicationId) {
        ApplicationSnapshot application = applicationCatalogClient.getApplication(applicationId);
        if (application.applicationId() == null || !applicationId.equals(application.applicationId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Application not found with id: " + applicationId);
        }
        if (!APPLICATION_SCHEDULABLE_STATUSES.contains(normalizeExternalStatus(application.status()))) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "Interviews can only be scheduled for SHORTLISTED or INTERVIEW_SCHEDULED applications"
            );
        }
        return application;
    }

    private Interview getRequiredInterview(Integer interviewId) {
        return interviewRepository.findById(interviewId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Interview not found with id: " + interviewId));
    }

    private void validateRecruiterOwnership(ProfileSnapshot recruiter, JobSnapshot job) {
        if (!RECRUITER.equalsIgnoreCase(recruiter.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only recruiters can manage interviews");
        }
        if (job.jobId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Job not found for the application");
        }
        if (!recruiter.profileId().equals(job.postedBy())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Recruiters can only manage interviews for their own jobs");
        }
    }

    private void validateCandidateOwnership(ProfileSnapshot candidate, ApplicationSnapshot application) {
        if (!CANDIDATE.equalsIgnoreCase(candidate.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only candidates can confirm or request rescheduling");
        }
        if (!candidate.profileId().equals(application.candidateId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Candidates can only manage their own interviews");
        }
    }

    private void validateReadAccess(ApplicationSnapshot application) {
        String role = currentRole();
        if (RECRUITER.equals(role)) {
            ProfileSnapshot recruiter = requireAuthenticatedProfile(RECRUITER);
            JobSnapshot job = jobCatalogClient.getJob(application.jobId());
            validateRecruiterOwnership(recruiter, job);
            return;
        }
        if (CANDIDATE.equals(role)) {
            ProfileSnapshot candidate = requireAuthenticatedProfile(CANDIDATE);
            validateCandidateOwnership(candidate, application);
            return;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
    }

    private ProfileSnapshot requireAuthenticatedProfile(String requiredRole) {
        ProfileSnapshot profile = profileDirectoryClient.getProfileByEmail(currentEmail());
        if (!requiredRole.equalsIgnoreCase(profile.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return profile;
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

    private LocalDateTime validateFutureDateTime(LocalDateTime scheduledAt) {
        if (scheduledAt == null || !scheduledAt.isAfter(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "scheduledAt must be in the future");
        }
        return scheduledAt;
    }

    private String normalizeMode(String mode) {
        String normalizedMode = mode == null ? null : mode.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        if (!VALID_MODES.contains(normalizedMode)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported interview mode: " + mode);
        }
        return normalizedMode;
    }

    private String normalizeStatus(String status) {
        String normalizedStatus = normalizeExternalStatus(status);
        if (!VALID_STATUSES.contains(normalizedStatus)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported interview status: " + status);
        }
        return normalizedStatus;
    }

    private String normalizeExternalStatus(String status) {
        return status == null ? null : status.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    private void validateModeSpecificFields(String mode, String meetLink, String location) {
        if (MODE_ONLINE.equals(mode) && !StringUtils.hasText(meetLink)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "meetLink is required for ONLINE interviews");
        }
        if (MODE_IN_PERSON.equals(mode) && !StringUtils.hasText(location)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "location is required for IN_PERSON interviews");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private InterviewResponse toResponse(Interview interview) {
        return new InterviewResponse(
            interview.getInterviewId(),
            interview.getApplicationId(),
            interview.getScheduledAt(),
            interview.getMode(),
            interview.getMeetLink(),
            interview.getLocation(),
            interview.getStatus(),
            interview.getNotes()
        );
    }

    private void publishInterviewEvent(
        String eventType,
        List<Integer> recipients,
        List<String> recipientEmails,
        String message,
        String emailSubject,
        String emailBody,
        Interview interview,
        ApplicationSnapshot application,
        JobSnapshot job
    ) {
        notificationEventPublisher.publish(new NotificationEvent(
            eventType,
            "INTERVIEW",
            message,
            recipients,
            recipientEmails,
            null,
            emailSubject,
            emailBody,
            application.applicationId(),
            application.jobId(),
            job.postedBy(),
            application.candidateId(),
            "INTERVIEW_SCHEDULED",
            null,
            LocalDateTime.now()
        ));
    }
}
