package com.hireconnect.web.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import com.hireconnect.web.dto.JobForm;
import com.hireconnect.web.dto.JobResponse;
import com.hireconnect.web.dto.PortalSession;
import com.hireconnect.web.support.GatewayClient;

@Service
public class JobService {

    private final GatewayClient gatewayClient;

    public JobService(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    public List<JobResponse> searchJobs(
        String title,
        String location,
        Double salaryMin,
        Double salaryMax,
        PortalSession session
    ) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        addParam(params, "title", title);
        addParam(params, "location", location);
        if (salaryMin != null) {
            params.add("salaryMin", salaryMin.toString());
        }
        if (salaryMax != null) {
            params.add("salaryMax", salaryMax.toString());
        }
        String path = UriComponentsBuilder.fromPath("/api/v1/jobs").queryParams(params).build().toUriString();
        return gatewayClient.get(path, session, new ParameterizedTypeReference<>() {
        });
    }

    public List<JobResponse> getAllJobs(PortalSession session) {
        return gatewayClient.get("/api/v1/jobs", session, new ParameterizedTypeReference<>() {
        });
    }

    public List<JobResponse> getJobsByRecruiter(Integer recruiterId, PortalSession session) {
        return gatewayClient.get("/api/v1/jobs/recruiter/{id}", session, new ParameterizedTypeReference<>() {
        }, recruiterId);
    }

    public JobResponse getJob(Integer jobId, PortalSession session) {
        return gatewayClient.get("/api/v1/jobs/{jobId}", session, JobResponse.class, jobId);
    }

    public JobResponse createJob(JobForm form, Integer postedBy, PortalSession session) {
        return gatewayClient.post("/api/v1/jobs", session, payload(form, postedBy), JobResponse.class);
    }

    public JobResponse updateJob(Integer jobId, JobForm form, PortalSession session) {
        return gatewayClient.put("/api/v1/jobs/{jobId}", session, payload(form, null), JobResponse.class, jobId);
    }

    public void deleteJob(Integer jobId, PortalSession session) {
        gatewayClient.delete("/api/v1/jobs/{jobId}", session, jobId);
    }

    public JobForm toForm(JobResponse job) {
        JobForm form = new JobForm();
        form.setTitle(job.title());
        form.setCategory(job.category());
        form.setType(job.type());
        form.setLocation(job.location());
        form.setSalaryMin(job.salaryMin());
        form.setSalaryMax(job.salaryMax());
        form.setDescription(job.description());
        form.setSkills(job.skills() == null ? "" : String.join(", ", job.skills()));
        form.setExperienceRequired(job.experienceRequired());
        form.setStatus(job.status());
        return form;
    }

    private Map<String, Object> payload(JobForm form, Integer postedBy) {
        java.util.LinkedHashMap<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("title", form.getTitle());
        payload.put("category", form.getCategory());
        payload.put("type", form.getType());
        payload.put("location", form.getLocation());
        payload.put("salaryMin", form.getSalaryMin());
        payload.put("salaryMax", form.getSalaryMax());
        payload.put("description", form.getDescription());
        payload.put("skills", Stream.of(form.getSkills().split(",")).map(String::trim).filter(StringUtils::hasText).toList());
        payload.put("experienceRequired", form.getExperienceRequired());
        if (postedBy != null) {
            payload.put("postedBy", postedBy);
            payload.put("postedAt", LocalDate.now());
        }
        payload.put("status", form.getStatus());
        return payload;
    }

    private void addParam(MultiValueMap<String, String> params, String key, String value) {
        if (StringUtils.hasText(value)) {
            params.add(key, value.trim());
        }
    }
}
