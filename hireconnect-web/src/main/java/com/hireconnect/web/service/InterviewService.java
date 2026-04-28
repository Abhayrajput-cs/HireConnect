package com.hireconnect.web.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import com.hireconnect.web.dto.ApplicationResponse;
import com.hireconnect.web.dto.InterviewResponse;
import com.hireconnect.web.dto.InterviewScheduleForm;
import com.hireconnect.web.dto.PortalSession;
import com.hireconnect.web.support.GatewayClient;

@Service
public class InterviewService {

    private final GatewayClient gatewayClient;
    private final ApplicationService applicationService;

    public InterviewService(GatewayClient gatewayClient, ApplicationService applicationService) {
        this.gatewayClient = gatewayClient;
        this.applicationService = applicationService;
    }

    public InterviewResponse scheduleInterview(InterviewScheduleForm form, PortalSession session) {
        return gatewayClient.post("/api/v1/interviews", session, Map.of(
            "applicationId", form.getApplicationId(),
            "scheduledAt", LocalDateTime.parse(form.getScheduledAt()),
            "mode", form.getMode(),
            "meetLink", form.getMeetLink(),
            "location", form.getLocation(),
            "notes", form.getNotes()
        ), InterviewResponse.class);
    }

    public List<InterviewResponse> getByCandidate(Integer candidateId, PortalSession session) {
        List<ApplicationResponse> applications = applicationService.getByCandidate(candidateId, session);
        List<InterviewResponse> interviews = new ArrayList<>();
        for (ApplicationResponse application : applications) {
            interviews.addAll(getByApplication(application.applicationId(), session));
        }
        return interviews;
    }

    public List<InterviewResponse> getByApplication(Integer applicationId, PortalSession session) {
        return gatewayClient.get("/api/v1/interviews/application/{applicationId}", session, new ParameterizedTypeReference<>() {
        }, applicationId);
    }

    public String confirmInterview(Integer interviewId, PortalSession session) {
        return gatewayClient.patch("/api/v1/interviews/{interviewId}/confirm", session, null, String.class, interviewId);
    }

    public InterviewResponse rescheduleInterview(Integer interviewId, LocalDateTime scheduledAt, String notes, PortalSession session) {
        return gatewayClient.patch("/api/v1/interviews/{interviewId}/reschedule", session, Map.of(
            "scheduledAt", scheduledAt,
            "notes", notes
        ), InterviewResponse.class, interviewId);
    }
}
