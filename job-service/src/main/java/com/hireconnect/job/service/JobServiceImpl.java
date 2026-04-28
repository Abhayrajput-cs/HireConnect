package com.hireconnect.job.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireconnect.job.client.RecruiterDirectoryClient;
import com.hireconnect.job.client.RecruiterProfileSnapshot;
import com.hireconnect.job.domain.Job;
import com.hireconnect.job.dto.JobRequest;
import com.hireconnect.job.dto.JobResponse;
import com.hireconnect.job.exception.ApiException;
import com.hireconnect.job.messaging.NotificationEvent;
import com.hireconnect.job.messaging.NotificationEventPublisher;
import com.hireconnect.job.repository.JobRepository;
import com.hireconnect.job.repository.JobSpecifications;

@Service
@Transactional
public class JobServiceImpl implements JobService {

    private static final String RECRUITER = "RECRUITER";
    private static final String DEFAULT_STATUS = "OPEN";

    private final JobRepository jobRepository;
    private final RecruiterDirectoryClient recruiterDirectoryClient;
    private final ObjectMapper objectMapper;
    private final NotificationEventPublisher notificationEventPublisher;

    public JobServiceImpl(
        JobRepository jobRepository,
        RecruiterDirectoryClient recruiterDirectoryClient,
        ObjectMapper objectMapper,
        NotificationEventPublisher notificationEventPublisher
    ) {
        this.jobRepository = jobRepository;
        this.recruiterDirectoryClient = recruiterDirectoryClient;
        this.objectMapper = objectMapper;
        this.notificationEventPublisher = notificationEventPublisher;
    }

    @Override
    @CacheEvict(
        cacheNames = {
            "jobAll", "jobById", "jobByTitle", "jobsByCategory", "jobsByLocation", "jobsByStatus",
            "jobsByRecruiter", "jobSearchResults"
        },
        allEntries = true
    )
    public JobResponse addJob(JobRequest request) {
        validateSalaryRange(request.salaryMin(), request.salaryMax());
        ensureRecruiterOwner(request.postedBy());

        Job job = new Job();
        job.setTitle(requiredText(request.title(), "title"));
        job.setCategory(requiredText(request.category(), "category"));
        job.setType(requiredText(request.type(), "type"));
        job.setLocation(requiredText(request.location(), "location"));
        job.setSalaryMin(request.salaryMin());
        job.setSalaryMax(request.salaryMax());
        job.setDescription(requiredText(request.description(), "description"));
        job.setSkills(normalizeSkills(request.skills()));
        job.setExperienceRequired(request.experienceRequired());
        job.setPostedBy(request.postedBy());
        job.setStatus(normalizeStatus(request.status()));
        job.setPostedAt(request.postedAt() == null ? LocalDate.now() : request.postedAt());

        Job savedJob = jobRepository.save(job);
        List<RecruiterProfileSnapshot> candidates = recruiterDirectoryClient.getProfilesByRole("CANDIDATE");
        notificationEventPublisher.publish(new NotificationEvent(
            "JOB_CREATED",
            "JOB_ALERT",
            "New job alert: " + savedJob.getTitle() + " in " + savedJob.getLocation(),
            candidates.stream().map(RecruiterProfileSnapshot::profileId).toList(),
            candidates.stream().map(RecruiterProfileSnapshot::email).filter(email -> email != null && !email.isBlank()).toList(),
            null,
            "New HireConnect job alert",
            "A new job has been posted: " + savedJob.getTitle() + " in " + savedJob.getLocation(),
            null,
            savedJob.getJobId(),
            savedJob.getPostedBy(),
            null,
            savedJob.getStatus(),
            savedJob.getPostedAt(),
            java.time.LocalDateTime.now()
        ));
        return toResponse(savedJob);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "jobAll")
    public List<JobResponse> getAllJobs() {
        return jobRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "jobById", key = "#jobId")
    public JobResponse getJobById(Integer jobId) {
        return toResponse(loadJob(jobId));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "jobByTitle", key = "#title.trim().toLowerCase()")
    public JobResponse getJobByTitle(String title) {
        return toResponse(jobRepository.findByTitle(requiredText(title, "title"))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Job not found with title: " + title)));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "jobsByCategory", key = "#category.trim().toLowerCase()")
    public List<JobResponse> getJobsByCategory(String category) {
        return jobRepository.findByCategoryIgnoreCase(requiredText(category, "category")).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "jobsByLocation", key = "#location.trim().toLowerCase()")
    public List<JobResponse> getJobsByLocation(String location) {
        return jobRepository.findByLocationContainingIgnoreCase(requiredText(location, "location")).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "jobsByStatus", key = "#status.trim().toUpperCase()")
    public List<JobResponse> getJobsByStatus(String status) {
        return jobRepository.findByStatusIgnoreCase(normalizeStatus(status)).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "jobsByRecruiter", key = "#postedBy")
    public List<JobResponse> getJobsByRecruiter(Integer postedBy) {
        return jobRepository.findByPostedBy(postedBy).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "jobSearchResults")
    public List<JobResponse> searchJobs(
        String title,
        String category,
        String location,
        Double salaryMin,
        Double salaryMax,
        Integer experienceRequired,
        String status,
        Integer postedBy
    ) {
        validateSalaryRange(salaryMin, salaryMax);
        return jobRepository.findAll(JobSpecifications.withFilters(
            title,
            category,
            location,
            salaryMin,
            salaryMax,
            experienceRequired,
            status,
            postedBy
        )).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @CacheEvict(
        cacheNames = {
            "jobAll", "jobById", "jobByTitle", "jobsByCategory", "jobsByLocation", "jobsByStatus",
            "jobsByRecruiter", "jobSearchResults"
        },
        allEntries = true
    )
    public JobResponse updateJob(Integer jobId, Map<String, Object> updates) {
        Job job = loadJob(jobId);

        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            applyUpdate(job, entry.getKey(), entry.getValue());
        }

        validateSalaryRange(job.getSalaryMin(), job.getSalaryMax());
        return toResponse(jobRepository.save(job));
    }

    @Override
    @CacheEvict(
        cacheNames = {
            "jobAll", "jobById", "jobByTitle", "jobsByCategory", "jobsByLocation", "jobsByStatus",
            "jobsByRecruiter", "jobSearchResults"
        },
        allEntries = true
    )
    public void deleteJob(Integer jobId) {
        jobRepository.delete(loadJob(jobId));
    }

    private void applyUpdate(Job job, String field, Object rawValue) {
        switch (field) {
            case "title" -> job.setTitle(requiredText(objectMapper.convertValue(rawValue, String.class), "title"));
            case "category" -> job.setCategory(requiredText(objectMapper.convertValue(rawValue, String.class), "category"));
            case "type" -> job.setType(requiredText(objectMapper.convertValue(rawValue, String.class), "type"));
            case "location" -> job.setLocation(requiredText(objectMapper.convertValue(rawValue, String.class), "location"));
            case "salaryMin" -> job.setSalaryMin(objectMapper.convertValue(rawValue, Double.class));
            case "salaryMax" -> job.setSalaryMax(objectMapper.convertValue(rawValue, Double.class));
            case "description" -> job.setDescription(requiredText(objectMapper.convertValue(rawValue, String.class), "description"));
            case "skills" -> job.setSkills(normalizeSkills(objectMapper.convertValue(rawValue, new TypeReference<List<String>>() {
            })));
            case "experienceRequired" -> job.setExperienceRequired(objectMapper.convertValue(rawValue, Integer.class));
            case "postedBy" -> {
                Integer postedBy = objectMapper.convertValue(rawValue, Integer.class);
                ensureRecruiterOwner(postedBy);
                job.setPostedBy(postedBy);
            }
            case "status" -> job.setStatus(normalizeStatus(objectMapper.convertValue(rawValue, String.class)));
            case "postedAt" -> job.setPostedAt(objectMapper.convertValue(rawValue, LocalDate.class));
            case "jobId" -> throw new ApiException(HttpStatus.BAD_REQUEST, "Field cannot be updated: " + field);
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported update field: " + field);
        }
    }

    private Job loadJob(Integer jobId) {
        return jobRepository.findById(jobId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Job not found with id: " + jobId));
    }

    private void ensureRecruiterOwner(Integer postedBy) {
        if (postedBy == null || postedBy <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "postedBy must be a positive recruiter profile id");
        }

        RecruiterProfileSnapshot recruiterProfile = recruiterDirectoryClient.getRecruiterProfile(postedBy);
        if (recruiterProfile == null || recruiterProfile.role() == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Profile service returned incomplete recruiter details");
        }
        if (!RECRUITER.equals(recruiterProfile.role().trim().toUpperCase(Locale.ROOT))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "postedBy must reference a recruiter profile");
        }
    }

    private void validateSalaryRange(Double salaryMin, Double salaryMax) {
        if (salaryMin == null || salaryMax == null) {
            return;
        }
        if (salaryMin < 0 || salaryMax < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Salary values cannot be negative");
        }
        if (salaryMax < salaryMin) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "salaryMax must be greater than or equal to salaryMin");
        }
    }

    private String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Field is required: " + field);
        }
        return value.trim();
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return DEFAULT_STATUS;
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private List<String> normalizeSkills(List<String> skills) {
        List<String> normalizedSkills = new ArrayList<>();
        if (skills == null || skills.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "At least one skill is required");
        }

        for (String skill : skills) {
            if (skill == null || skill.isBlank()) {
                continue;
            }
            normalizedSkills.add(skill.trim());
        }

        if (normalizedSkills.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "At least one skill is required");
        }
        return normalizedSkills;
    }

    private JobResponse toResponse(Job job) {
        return new JobResponse(
            job.getJobId(),
            job.getTitle(),
            job.getCategory(),
            job.getType(),
            job.getLocation(),
            job.getSalaryMin(),
            job.getSalaryMax(),
            job.getDescription(),
            List.copyOf(job.getSkills()),
            job.getExperienceRequired(),
            job.getPostedBy(),
            job.getStatus(),
            job.getPostedAt()
        );
    }
}
