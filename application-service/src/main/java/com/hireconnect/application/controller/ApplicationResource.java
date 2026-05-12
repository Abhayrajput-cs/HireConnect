package com.hireconnect.application.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hireconnect.application.dto.ApplicationRequest;
import com.hireconnect.application.dto.ApplicationResponse;
import com.hireconnect.application.dto.StatusUpdateRequest;
import com.hireconnect.application.service.ApplicationService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationResource {

    private final ApplicationService applicationService;

    public ApplicationResource(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<ApplicationResponse> submit(@Valid @RequestBody ApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationService.submitApplication(request));
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<ApplicationResponse> getById(@PathVariable Integer applicationId) {
        return ResponseEntity.ok(applicationService.getById(applicationId));
    }

    @GetMapping("/candidate/{candidateId}")
    public ResponseEntity<List<ApplicationResponse>> getByCandidate(@PathVariable Integer candidateId) {
        return ResponseEntity.ok(applicationService.getByCandidate(candidateId));
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<ApplicationResponse>> getByJob(@PathVariable Integer jobId) {
        return ResponseEntity.ok(applicationService.getByJob(jobId));
    }

    @GetMapping("/job/{jobId}/count")
    public ResponseEntity<Integer> countByJob(@PathVariable Integer jobId) {
        return ResponseEntity.ok(applicationService.countByJob(jobId));
    }

    @GetMapping
    public ResponseEntity<List<ApplicationResponse>> search(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appliedFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appliedTo
    ) {
        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(applicationService.getByStatus(status));
        }
        if (appliedFrom != null && appliedTo != null) {
            return ResponseEntity.ok(applicationService.getByAppliedDateRange(appliedFrom, appliedTo));
        }
        return ResponseEntity.badRequest().build();
    }

    @PatchMapping("/{applicationId}/status")
    public ResponseEntity<ApplicationResponse> updateStatus(
        @PathVariable Integer applicationId,
        @Valid @RequestBody StatusUpdateRequest request
    ) {
        return ResponseEntity.ok(applicationService.updateStatus(applicationId, request.status()));
    }

    @PatchMapping("/{applicationId}/withdraw")
    public ResponseEntity<ApplicationResponse> withdraw(@PathVariable Integer applicationId) {
        return ResponseEntity.ok(applicationService.withdrawApplication(applicationId));
    }

    @PatchMapping("/{applicationId}/offer/accept")
    public ResponseEntity<ApplicationResponse> acceptOffer(@PathVariable Integer applicationId) {
        return ResponseEntity.ok(applicationService.acceptOffer(applicationId));
    }

    @PatchMapping("/{applicationId}/offer/decline")
    public ResponseEntity<ApplicationResponse> declineOffer(@PathVariable Integer applicationId) {
        return ResponseEntity.ok(applicationService.declineOffer(applicationId));
    }
}
