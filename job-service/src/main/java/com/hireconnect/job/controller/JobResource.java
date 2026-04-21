package com.hireconnect.job.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hireconnect.job.dto.JobRequest;
import com.hireconnect.job.dto.JobResponse;
import com.hireconnect.job.service.JobService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/v1/jobs")
public class JobResource {

    private final JobService jobService;

    public JobResource(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<JobResponse> addJob(@Valid @RequestBody JobRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.addJob(request));
    }

    @GetMapping
    public ResponseEntity<List<JobResponse>> getJobs(
        @RequestParam(required = false) String title,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String location,
        @RequestParam(required = false) Double salaryMin,
        @RequestParam(required = false) Double salaryMax,
        @RequestParam(required = false) Integer experienceRequired,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Integer postedBy
    ) {
        if (title == null
            && category == null
            && location == null
            && salaryMin == null
            && salaryMax == null
            && experienceRequired == null
            && status == null
            && postedBy == null) {
            return ResponseEntity.ok(jobService.getAllJobs());
        }

        return ResponseEntity.ok(jobService.searchJobs(
            title,
            category,
            location,
            salaryMin,
            salaryMax,
            experienceRequired,
            status,
            postedBy
        ));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<JobResponse> getJobById(@PathVariable Integer jobId) {
        return ResponseEntity.ok(jobService.getJobById(jobId));
    }

    @GetMapping("/title/{title}")
    public ResponseEntity<JobResponse> getJobByTitle(@PathVariable String title) {
        return ResponseEntity.ok(jobService.getJobByTitle(title));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<JobResponse>> getJobsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(jobService.getJobsByCategory(category));
    }

    @GetMapping("/location/{location}")
    public ResponseEntity<List<JobResponse>> getJobsByLocation(@PathVariable String location) {
        return ResponseEntity.ok(jobService.getJobsByLocation(location));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<JobResponse>> getJobsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(jobService.getJobsByStatus(status));
    }

    @GetMapping("/recruiter/{postedBy}")
    public ResponseEntity<List<JobResponse>> getJobsByRecruiter(@PathVariable Integer postedBy) {
        return ResponseEntity.ok(jobService.getJobsByRecruiter(postedBy));
    }

    @PutMapping("/{jobId}")
    public ResponseEntity<JobResponse> updateJob(@PathVariable Integer jobId, @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(jobService.updateJob(jobId, updates));
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> deleteJob(@PathVariable Integer jobId) {
        jobService.deleteJob(jobId);
        return ResponseEntity.noContent().build();
    }
}
