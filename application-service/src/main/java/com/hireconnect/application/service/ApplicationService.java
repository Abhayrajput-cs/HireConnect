package com.hireconnect.application.service;

import java.time.LocalDate;
import java.util.List;

import com.hireconnect.application.dto.ApplicationRequest;
import com.hireconnect.application.dto.ApplicationResponse;

public interface ApplicationService {

    ApplicationResponse submitApplication(ApplicationRequest request);

    List<ApplicationResponse> getByCandidate(Integer candidateId);

    List<ApplicationResponse> getByJob(Integer jobId);

    List<ApplicationResponse> getByStatus(String status);

    List<ApplicationResponse> getByAppliedDateRange(LocalDate startDate, LocalDate endDate);

    ApplicationResponse getById(Integer applicationId);

    ApplicationResponse updateStatus(Integer applicationId, String status);

    ApplicationResponse withdrawApplication(Integer applicationId);

    int countByJob(Integer jobId);
}
