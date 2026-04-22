package com.hireconnect.web.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hireconnect.web.domain.SubscriptionPlan;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {

    List<SubscriptionPlan> findByActiveTrueOrderByMonthlyPriceAsc();

    Optional<SubscriptionPlan> findByCode(String code);
}
