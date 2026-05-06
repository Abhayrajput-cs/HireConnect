package com.hireconnect.job.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.hireconnect.job.domain.Job;

public interface JobRepository extends JpaRepository<Job, Integer>, JpaSpecificationExecutor<Job> {

    Optional<Job> findByTitle(String title);

    List<Job> findByCategoryIgnoreCase(String category);

    List<Job> findByLocationContainingIgnoreCase(String location);

    List<Job> findByPostedBy(Integer postedBy);

    List<Job> findByStatusIgnoreCase(String status);

    List<Job> findByTitleContainingIgnoreCase(String title);

    int countByPostedBy(Integer postedBy);
}
