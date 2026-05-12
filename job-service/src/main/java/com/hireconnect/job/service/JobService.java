package com.hireconnect.job.service;

import java.util.List;
import java.util.Map;

import com.hireconnect.job.dto.JobRequest;
import com.hireconnect.job.dto.JobResponse;

public interface JobService {

    JobResponse addJob(JobRequest request);

    List<JobResponse> getAllJobs();

    JobResponse getJobById(Integer jobId);

    JobResponse getJobByTitle(String title);

    List<JobResponse> getJobsByCategory(String category);

    List<JobResponse> getJobsByLocation(String location);

    List<JobResponse> getJobsByStatus(String status);

    List<JobResponse> getJobsByRecruiter(Integer postedBy);

    List<JobResponse> searchJobs(
        String title,
        String category,
        String location,
        Double salaryMin,
        Double salaryMax,
        Integer experienceRequired,
        String status,
        Integer postedBy
    );

    JobResponse updateJob(Integer jobId, Map<String, Object> updates);

    void deleteJob(Integer jobId);
}
