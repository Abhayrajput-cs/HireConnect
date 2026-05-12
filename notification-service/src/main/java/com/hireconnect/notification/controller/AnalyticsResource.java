package com.hireconnect.notification.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hireconnect.notification.dto.AnalyticsSummary;
import com.hireconnect.notification.dto.JobViewRequest;
import com.hireconnect.notification.service.AnalyticsService;

@Validated
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsResource {

    private final AnalyticsService analyticsService;

    public AnalyticsResource(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PostMapping("/jobs/{jobId}/views")
    public ResponseEntity<Long> recordJobView(@PathVariable Integer jobId, @RequestBody(required = false) JobViewRequest request) {
        return ResponseEntity.ok(analyticsService.recordJobView(jobId));
    }

    @GetMapping("/jobs/{jobId}/view-count")
    public ResponseEntity<Integer> getJobViewCount(@PathVariable Integer jobId) {
        return ResponseEntity.ok(analyticsService.getJobViewCount(jobId));
    }

    @GetMapping("/jobs/{jobId}/application-count")
    public ResponseEntity<Integer> getApplicationCount(@PathVariable Integer jobId) {
        return ResponseEntity.ok(analyticsService.getAppCountByJob(jobId));
    }

    @GetMapping("/jobs/{jobId}/view-to-apply-ratio")
    public ResponseEntity<Double> getViewToApplyRatio(@PathVariable Integer jobId) {
        return ResponseEntity.ok(analyticsService.getViewToApplyRatio(jobId));
    }

    @GetMapping("/recruiter/{recruiterId}")
    public ResponseEntity<AnalyticsSummary> getRecruiterStats(@PathVariable Integer recruiterId) {
        return ResponseEntity.ok(analyticsService.getPipelineStats(recruiterId));
    }

    @GetMapping("/recruiter/{recruiterId}/time-to-hire")
    public ResponseEntity<Double> getTimeToHire(@PathVariable Integer recruiterId) {
        return ResponseEntity.ok(analyticsService.getTimeToHire(recruiterId));
    }

    @GetMapping("/admin")
    public ResponseEntity<AnalyticsSummary> getPlatformStats() {
        return ResponseEntity.ok(analyticsService.getPlatformStats());
    }

    @GetMapping("/categories/top")
    public ResponseEntity<Map<String, Long>> getTopJobCategories() {
        return ResponseEntity.ok(analyticsService.getTopJobCategories());
    }
}
