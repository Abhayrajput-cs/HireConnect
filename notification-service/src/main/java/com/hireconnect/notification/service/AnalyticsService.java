package com.hireconnect.notification.service;

import java.util.Map;

import com.hireconnect.notification.dto.AnalyticsSummary;

public interface AnalyticsService {

    int getJobViewCount(Integer jobId);

    int getAppCountByJob(Integer jobId);

    double getViewToApplyRatio(Integer jobId);

    double getTimeToHire(Integer recruiterId);

    AnalyticsSummary getPipelineStats(Integer recruiterId);

    AnalyticsSummary getPlatformStats();

    Map<String, Long> getTopJobCategories();

    long recordJobView(Integer jobId);
}
