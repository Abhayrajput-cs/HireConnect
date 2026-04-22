package com.hireconnect.interview.service;

import java.time.LocalDateTime;
import java.util.List;

import com.hireconnect.interview.dto.InterviewRescheduleRequest;
import com.hireconnect.interview.dto.InterviewResponse;
import com.hireconnect.interview.dto.InterviewScheduleRequest;

public interface InterviewService {

    InterviewResponse scheduleInterview(InterviewScheduleRequest request);

    String confirmInterview(Integer interviewId);

    InterviewResponse rescheduleInterview(Integer interviewId, InterviewRescheduleRequest request);

    void cancelInterview(Integer interviewId);

    List<InterviewResponse> getByApplication(Integer applicationId);

    List<InterviewResponse> getByStatus(String status);

    List<InterviewResponse> getByScheduledRange(LocalDateTime scheduledFrom, LocalDateTime scheduledTo);

    InterviewResponse getById(Integer interviewId);
}
