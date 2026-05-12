package com.hireconnect.web.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import com.hireconnect.web.dto.ApplicationResponse;
import com.hireconnect.web.dto.JobResponse;
import com.hireconnect.web.dto.PortalSession;
import com.hireconnect.web.dto.ProfileResponse;
import com.hireconnect.web.support.GatewayClient;

@Service
public class ApplicationService {

    private final GatewayClient gatewayClient;
    private final JobService jobService;

    public ApplicationService(GatewayClient gatewayClient, JobService jobService) {
        this.gatewayClient = gatewayClient;
        this.jobService = jobService;
    }

    public ApplicationResponse apply(Integer jobId, Integer candidateId, String resumeUrl, PortalSession session) {
        return gatewayClient.post("/api/v1/applications", session, Map.of(
            "jobId", jobId,
            "candidateId", candidateId,
            "coverLetter", "Applied via HireConnect web portal",
            "resumeUrl", resumeUrl
        ), ApplicationResponse.class);
    }

    public List<ApplicationResponse> getByCandidate(Integer candidateId, PortalSession session) {
        return gatewayClient.get("/api/v1/applications/candidate/{candidateId}", session, new ParameterizedTypeReference<>() {
        }, candidateId);
    }

    public List<ApplicationResponse> getByJob(Integer jobId, PortalSession session) {
        return gatewayClient.get("/api/v1/applications/job/{jobId}", session, new ParameterizedTypeReference<>() {
        }, jobId);
    }

    public List<ApplicationResponse> getByRecruiter(Integer recruiterId, PortalSession session) {
        List<JobResponse> jobs = jobService.getJobsByRecruiter(recruiterId, session);
        List<ApplicationResponse> applications = new ArrayList<>();
        for (JobResponse job : jobs) {
            applications.addAll(getByJob(job.jobId(), session));
        }
        return applications;
    }

    public ApplicationResponse updateStatus(Integer applicationId, String status, PortalSession session) {
        return gatewayClient.patch("/api/v1/applications/{applicationId}/status", session, Map.of("status", status), ApplicationResponse.class, applicationId);
    }

    public ApplicationResponse shortlistCandidate(Integer applicationId, PortalSession session) {
        return updateStatus(applicationId, "SHORTLISTED", session);
    }

    public ApplicationResponse rejectCandidate(Integer applicationId, PortalSession session) {
        return updateStatus(applicationId, "REJECTED", session);
    }

    public ApplicationResponse withdraw(Integer applicationId, PortalSession session) {
        return gatewayClient.patch("/api/v1/applications/{applicationId}/withdraw", session, null, ApplicationResponse.class, applicationId);
    }

    public ApplicationResponse getById(Integer applicationId, PortalSession session) {
        return gatewayClient.get("/api/v1/applications/{applicationId}", session, ApplicationResponse.class, applicationId);
    }
}
