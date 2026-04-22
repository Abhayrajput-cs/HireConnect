package com.hireconnect.web.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hireconnect.web.domain.RecruiterSubscription;

public interface RecruiterSubscriptionRepository extends JpaRepository<RecruiterSubscription, Long> {

    Optional<RecruiterSubscription> findFirstByRecruiterProfileIdOrderByUpdatedAtDesc(Integer recruiterProfileId);

    List<RecruiterSubscription> findAllByOrderByUpdatedAtDesc();
}
