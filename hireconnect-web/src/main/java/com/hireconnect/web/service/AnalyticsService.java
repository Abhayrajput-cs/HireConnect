package com.hireconnect.web.service;

import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import com.hireconnect.web.dto.AnalyticsSummary;
import com.hireconnect.web.dto.PortalSession;
import com.hireconnect.web.support.GatewayClient;

@Service
public class AnalyticsService {

    private final GatewayClient gatewayClient;

    public AnalyticsService(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    public AnalyticsSummary getRecruiterStats(Integer recruiterId, PortalSession session) {
        return gatewayClient.get("/api/v1/analytics/recruiter/{recruiterId}", session, AnalyticsSummary.class, recruiterId);
    }

    public Double getTimeToHire(Integer recruiterId, PortalSession session) {
        return gatewayClient.get("/api/v1/analytics/recruiter/{recruiterId}/time-to-hire", session, Double.class, recruiterId);
    }

    public AnalyticsSummary getPlatformStats(PortalSession session) {
        return gatewayClient.get("/api/v1/analytics/admin", session, AnalyticsSummary.class);
    }

    public Integer getJobViewCount(Integer jobId, PortalSession session) {
        return gatewayClient.get("/api/v1/analytics/jobs/{jobId}/view-count", session, Integer.class, jobId);
    }

    public Integer getAppCountByJob(Integer jobId, PortalSession session) {
        return gatewayClient.get("/api/v1/analytics/jobs/{jobId}/application-count", session, Integer.class, jobId);
    }

    public Double getViewToApplyRatio(Integer jobId, PortalSession session) {
        return gatewayClient.get("/api/v1/analytics/jobs/{jobId}/view-to-apply-ratio", session, Double.class, jobId);
    }

    public Map<String, Long> getTopJobCategories(PortalSession session) {
        return gatewayClient.get("/api/v1/analytics/categories/top", session, new ParameterizedTypeReference<>() {
        });
    }
}
