package com.hireconnect.notification.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.hireconnect.notification.client.JobServiceClient;
import com.hireconnect.notification.client.JobSnapshot;
import com.hireconnect.notification.client.ProfileServiceClient;
import com.hireconnect.notification.client.ProfileSnapshot;
import com.hireconnect.notification.domain.ApplicationMetric;
import com.hireconnect.notification.domain.JobViewMetric;
import com.hireconnect.notification.dto.AnalyticsSummary;
import com.hireconnect.notification.exception.ApiException;
import com.hireconnect.notification.repository.ApplicationMetricRepository;
import com.hireconnect.notification.repository.JobViewMetricRepository;

@Service
@Transactional
public class AnalyticsServiceImpl implements AnalyticsService {

    private final JobViewMetricRepository jobViewMetricRepository;
    private final ApplicationMetricRepository applicationMetricRepository;
    private final JobServiceClient jobServiceClient;
    private final ProfileServiceClient profileServiceClient;

    public AnalyticsServiceImpl(
        JobViewMetricRepository jobViewMetricRepository,
        ApplicationMetricRepository applicationMetricRepository,
        JobServiceClient jobServiceClient,
        ProfileServiceClient profileServiceClient
    ) {
        this.jobViewMetricRepository = jobViewMetricRepository;
        this.applicationMetricRepository = applicationMetricRepository;
        this.jobServiceClient = jobServiceClient;
        this.profileServiceClient = profileServiceClient;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "analyticsJobViewCount", key = "#jobId")
    public int getJobViewCount(Integer jobId) {
        validateJob(jobId);
        return Math.toIntExact(jobViewMetricRepository.findById(jobId).map(JobViewMetric::getViewCount).orElse(0L));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "analyticsAppCountByJob", key = "#jobId")
    public int getAppCountByJob(Integer jobId) {
        validateJob(jobId);
        return applicationMetricRepository.countByJobId(jobId);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "analyticsViewToApplyRatio", key = "#jobId")
    public double getViewToApplyRatio(Integer jobId) {
        int applications = getAppCountByJob(jobId);
        if (applications == 0) {
            return 0.0d;
        }
        return (double) getJobViewCount(jobId) / applications;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "analyticsTimeToHire", key = "#recruiterId")
    public double getTimeToHire(Integer recruiterId) {
        enforceRecruiterAccess(recruiterId);
        return averageTimeToHire(applicationMetricRepository.findByRecruiterIdAndOfferedAtIsNotNull(recruiterId));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "analyticsPipelineStats", key = "#recruiterId")
    public AnalyticsSummary getPipelineStats(Integer recruiterId) {
        enforceRecruiterAccess(recruiterId);
        List<JobSnapshot> jobs = jobServiceClient.getJobsByRecruiter(recruiterId);
        return buildSummary(jobs);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "analyticsPlatformStats")
    public AnalyticsSummary getPlatformStats() {
        return buildSummary(jobServiceClient.getAllJobs());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "analyticsTopJobCategories")
    public Map<String, Long> getTopJobCategories() {
        return jobServiceClient.getAllJobs().stream()
            .filter(job -> StringUtils.hasText(job.category()))
            .collect(Collectors.groupingBy(JobSnapshot::category, Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (left, right) -> left,
                LinkedHashMap::new
            ));
    }

    @Override
    @CacheEvict(
        cacheNames = {
            "analyticsJobViewCount", "analyticsViewToApplyRatio", "analyticsPipelineStats", "analyticsPlatformStats"
        },
        allEntries = true
    )
    public long recordJobView(Integer jobId) {
        validateJob(jobId);
        JobViewMetric metric = jobViewMetricRepository.findById(jobId).orElseGet(JobViewMetric::new);
        metric.setJobId(jobId);
        metric.setViewCount(metric.getViewCount() + 1);
        metric.setLastViewedAt(LocalDateTime.now());
        return jobViewMetricRepository.save(metric).getViewCount();
    }

    private AnalyticsSummary buildSummary(List<JobSnapshot> jobs) {
        AnalyticsSummary summary = new AnalyticsSummary();
        summary.setTotalJobs(jobs.size());

        int totalViews = 0;
        int totalApplications = 0;
        int shortlisted = 0;
        int offered = 0;
        int rejected = 0;

        for (JobSnapshot job : jobs) {
            totalViews += getJobViewCount(job.jobId());
            List<ApplicationMetric> applications = applicationMetricRepository.findByJobId(job.jobId());
            totalApplications += applications.size();
            shortlisted += countByStatus(applications, "SHORTLISTED");
            offered += countByStatus(applications, "OFFERED");
            rejected += countByStatus(applications, "REJECTED");
        }

        summary.setTotalApplications(totalApplications);
        summary.setShortlistedCount(shortlisted);
        summary.setOfferedCount(offered);
        summary.setRejectedCount(rejected);
        summary.setViewToApplyRatio(totalApplications == 0 ? 0.0d : (double) totalViews / totalApplications);

        List<Integer> recruiterIds = jobs.stream().map(JobSnapshot::postedBy).distinct().toList();
        if (recruiterIds.size() == 1) {
            summary.setAvgTimeToHireDays(getTimeToHire(recruiterIds.getFirst()));
        } else {
            summary.setAvgTimeToHireDays(averageTimeToHire(applicationMetricRepository.findByOfferedAtIsNotNull()));
        }
        return summary;
    }

    private double averageTimeToHire(List<ApplicationMetric> metrics) {
        return metrics.stream()
            .filter(metric -> metric.getAppliedAt() != null && metric.getOfferedAt() != null)
            .mapToLong(metric -> Duration.between(metric.getAppliedAt().atStartOfDay(), metric.getOfferedAt()).toDays())
            .average()
            .orElse(0.0d);
    }

    private int countByStatus(List<ApplicationMetric> applications, String status) {
        return (int) applications.stream()
            .filter(application -> status.equals(normalize(application.getLastStatus())))
            .count();
    }

    private void enforceRecruiterAccess(Integer recruiterId) {
        if ("ADMIN".equals(currentRole())) {
            return;
        }
        ProfileSnapshot currentProfile = profileServiceClient.getProfileByEmailForAuthUser(currentEmail());
        if (!"RECRUITER".equalsIgnoreCase(currentProfile.role()) || !recruiterId.equals(currentProfile.profileId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Recruiters can only view their own analytics");
        }
    }

    private void validateJob(Integer jobId) {
        if (jobId == null || jobServiceClient.getJob(jobId) == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Job not found with id: " + jobId);
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

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }
}
