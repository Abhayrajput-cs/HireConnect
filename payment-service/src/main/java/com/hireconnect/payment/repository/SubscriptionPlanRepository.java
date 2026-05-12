package com.hireconnect.payment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hireconnect.payment.domain.PlanType;
import com.hireconnect.payment.domain.SubscriptionPlan;
import com.hireconnect.payment.domain.UserRole;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {

    Optional<SubscriptionPlan> findByPlanTypeAndActiveTrue(PlanType planType);

    List<SubscriptionPlan> findByRoleAndActiveTrueOrderByAmountAsc(UserRole role);
}
