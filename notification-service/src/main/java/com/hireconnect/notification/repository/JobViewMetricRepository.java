package com.hireconnect.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hireconnect.notification.domain.JobViewMetric;

public interface JobViewMetricRepository extends JpaRepository<JobViewMetric, Integer> {
}
