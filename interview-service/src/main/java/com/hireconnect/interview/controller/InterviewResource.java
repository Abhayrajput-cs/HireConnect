package com.hireconnect.interview.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hireconnect.interview.dto.InterviewRescheduleRequest;
import com.hireconnect.interview.dto.InterviewResponse;
import com.hireconnect.interview.dto.InterviewScheduleRequest;
import com.hireconnect.interview.service.InterviewService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/v1/interviews")
public class InterviewResource {

    private final InterviewService interviewService;

    public InterviewResource(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @PostMapping
    public ResponseEntity<InterviewResponse> schedule(@Valid @RequestBody InterviewScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(interviewService.scheduleInterview(request));
    }

    @PatchMapping("/{interviewId}/confirm")
    public ResponseEntity<String> confirm(@PathVariable Integer interviewId) {
        return ResponseEntity.ok(interviewService.confirmInterview(interviewId));
    }

    @PatchMapping("/{interviewId}/reschedule")
    public ResponseEntity<InterviewResponse> reschedule(
        @PathVariable Integer interviewId,
        @Valid @RequestBody InterviewRescheduleRequest request
    ) {
        return ResponseEntity.ok(interviewService.rescheduleInterview(interviewId, request));
    }

    @PatchMapping("/{interviewId}/reschedule/accept")
    public ResponseEntity<InterviewResponse> acceptReschedule(@PathVariable Integer interviewId) {
        return ResponseEntity.ok(interviewService.acceptRescheduleRequest(interviewId));
    }

    @PatchMapping("/{interviewId}/reschedule/decline")
    public ResponseEntity<InterviewResponse> declineReschedule(@PathVariable Integer interviewId) {
        return ResponseEntity.ok(interviewService.declineRescheduleRequest(interviewId));
    }

    @DeleteMapping("/{interviewId}")
    public ResponseEntity<Void> cancel(@PathVariable Integer interviewId) {
        interviewService.cancelInterview(interviewId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{interviewId}")
    public ResponseEntity<InterviewResponse> getById(@PathVariable Integer interviewId) {
        return ResponseEntity.ok(interviewService.getById(interviewId));
    }

    @GetMapping("/application/{applicationId}")
    public ResponseEntity<List<InterviewResponse>> getByApplication(@PathVariable Integer applicationId) {
        return ResponseEntity.ok(interviewService.getByApplication(applicationId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<InterviewResponse>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(interviewService.getByStatus(status));
    }

    @GetMapping
    public ResponseEntity<List<InterviewResponse>> getByScheduledRange(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledFrom,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledTo
    ) {
        return ResponseEntity.ok(interviewService.getByScheduledRange(scheduledFrom, scheduledTo));
    }
}
