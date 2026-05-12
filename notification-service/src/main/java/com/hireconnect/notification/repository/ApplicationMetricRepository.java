package com.hireconnect.notification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hireconnect.notification.domain.ApplicationMetric;

public interface ApplicationMetricRepository extends JpaRepository<ApplicationMetric, Integer> {

    List<ApplicationMetric> findByJobId(Integer jobId);

    List<ApplicationMetric> findByRecruiterId(Integer recruiterId);

    List<ApplicationMetric> findByRecruiterIdAndOfferedAtIsNotNull(Integer recruiterId);

    List<ApplicationMetric> findByOfferedAtIsNotNull();

    int countByJobId(Integer jobId);
}
