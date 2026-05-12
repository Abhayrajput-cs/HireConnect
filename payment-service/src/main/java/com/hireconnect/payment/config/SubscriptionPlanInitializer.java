package com.hireconnect.payment.config;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.hireconnect.payment.domain.PlanType;
import com.hireconnect.payment.domain.SubscriptionPlan;
import com.hireconnect.payment.domain.UserRole;
import com.hireconnect.payment.repository.SubscriptionPlanRepository;

@Component
public class SubscriptionPlanInitializer implements CommandLineRunner {

    private final SubscriptionPlanRepository plans;

    public SubscriptionPlanInitializer(SubscriptionPlanRepository plans) {
        this.plans = plans;
    }

    @Override
    public void run(String... args) {
        upsert(PlanType.CANDIDATE_FREE, UserRole.CANDIDATE, "Candidate Free", BigDecimal.ZERO, 3650, false, List.of(
            "Browse jobs",
            "Apply to jobs",
            "Track applications"
        ));
        upsert(PlanType.CANDIDATE_PREMIUM, UserRole.CANDIDATE, "Candidate Premium", BigDecimal.valueOf(299), 30, true, List.of(
            "Featured profile",
            "Priority applications",
            "Application analytics",
            "Resume/profile visibility boost"
        ));
        upsert(PlanType.RECRUITER_FREE, UserRole.RECRUITER, "Recruiter Free", BigDecimal.ZERO, 3650, false, List.of(
            "Create recruiter profile",
            "Post limited jobs",
            "Review applicants"
        ));
        upsert(PlanType.RECRUITER_PREMIUM, UserRole.RECRUITER, "Recruiter Premium", BigDecimal.valueOf(999), 30, true, List.of(
            "Post more jobs",
            "Access premium candidate profiles",
            "Direct candidate contact",
            "Advanced filtering/search",
            "Analytics dashboard access"
        ));
    }

    private void upsert(
        PlanType planType,
        UserRole role,
        String displayName,
        BigDecimal amount,
        int durationDays,
        boolean premium,
        List<String> benefits
    ) {
        SubscriptionPlan plan = plans.findByPlanTypeAndActiveTrue(planType).orElseGet(SubscriptionPlan::new);
        plan.setPlanType(planType);
        plan.setRole(role);
        plan.setDisplayName(displayName);
        plan.setAmount(amount);
        plan.setCurrency("INR");
        plan.setDurationDays(durationDays);
        plan.setPremium(premium);
        plan.setActive(true);
        plan.setBenefits(benefits);
        plan.setUpdatedAt(Instant.now());
        plans.save(plan);
    }
}
