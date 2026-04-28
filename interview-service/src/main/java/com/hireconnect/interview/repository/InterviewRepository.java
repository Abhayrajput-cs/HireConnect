package com.hireconnect.interview.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hireconnect.interview.domain.Interview;

public interface InterviewRepository extends JpaRepository<Interview, Integer> {

    List<Interview> findByApplicationIdOrderByScheduledAtAsc(Integer applicationId);

    List<Interview> findByStatusOrderByScheduledAtAsc(String status);

    List<Interview> findByScheduledAtBetweenOrderByScheduledAtAsc(LocalDateTime start, LocalDateTime end);

    void deleteByInterviewId(Integer interviewId);
}
