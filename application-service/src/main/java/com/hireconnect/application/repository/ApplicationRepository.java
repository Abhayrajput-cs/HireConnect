package com.hireconnect.application.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hireconnect.application.domain.Application;

public interface ApplicationRepository extends JpaRepository<Application, Integer> {

    List<Application> findByCandidateIdOrderByAppliedAtDesc(Integer candidateId);

    List<Application> findByJobIdOrderByAppliedAtDesc(Integer jobId);

    List<Application> findByStatusOrderByAppliedAtDesc(String status);

    Optional<Application> findFirstByJobIdAndCandidateId(Integer jobId, Integer candidateId);

    List<Application> findByAppliedAtBetweenOrderByAppliedAtDesc(LocalDate startDate, LocalDate endDate);

    int countByJobId(Integer jobId);

    int countByCandidateId(Integer candidateId);
}
