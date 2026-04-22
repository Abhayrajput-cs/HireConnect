package com.hireconnect.web.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hireconnect.web.domain.InvoiceRecord;
import com.hireconnect.web.domain.RecruiterSubscription;
import com.hireconnect.web.domain.SubscriptionPlan;
import com.hireconnect.web.repository.InvoiceRecordRepository;
import com.hireconnect.web.repository.RecruiterSubscriptionRepository;
import com.hireconnect.web.repository.SubscriptionPlanRepository;

import jakarta.annotation.PostConstruct;

@Service
@Transactional
public class SubscriptionService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final RecruiterSubscriptionRepository recruiterSubscriptionRepository;
    private final InvoiceRecordRepository invoiceRecordRepository;

    public SubscriptionService(
        SubscriptionPlanRepository subscriptionPlanRepository,
        RecruiterSubscriptionRepository recruiterSubscriptionRepository,
        InvoiceRecordRepository invoiceRecordRepository
    ) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.recruiterSubscriptionRepository = recruiterSubscriptionRepository;
        this.invoiceRecordRepository = invoiceRecordRepository;
    }

    @PostConstruct
    void initializeDefaults() {
        createPlanIfMissing("FREE", "Starter", new BigDecimal("0.00"), "Basic job posting access for early recruiters");
        createPlanIfMissing("GROWTH", "Growth", new BigDecimal("2499.00"), "More job slots and recruiter analytics visibility");
        createPlanIfMissing("ENTERPRISE", "Enterprise", new BigDecimal("7999.00"), "Priority support, reporting, and multi-user access");
    }

    public List<SubscriptionPlan> getActivePlans() {
        return subscriptionPlanRepository.findByActiveTrueOrderByMonthlyPriceAsc();
    }

    public List<SubscriptionPlan> getAllPlans() {
        return subscriptionPlanRepository.findAll();
    }

    public RecruiterSubscription getCurrentSubscription(Integer recruiterProfileId) {
        return recruiterSubscriptionRepository.findFirstByRecruiterProfileIdOrderByUpdatedAtDesc(recruiterProfileId).orElse(null);
    }

    public RecruiterSubscription subscribe(Integer recruiterProfileId, String planCode, boolean autoRenew) {
        SubscriptionPlan plan = subscriptionPlanRepository.findByCode(planCode)
            .orElseThrow(() -> new IllegalArgumentException("Subscription plan not found: " + planCode));

        RecruiterSubscription subscription = recruiterSubscriptionRepository.findFirstByRecruiterProfileIdOrderByUpdatedAtDesc(recruiterProfileId)
            .orElseGet(RecruiterSubscription::new);
        subscription.setRecruiterProfileId(recruiterProfileId);
        subscription.setPlanCode(plan.getCode());
        subscription.setPlanName(plan.getName());
        subscription.setMonthlyPrice(plan.getMonthlyPrice());
        subscription.setStatus(plan.getMonthlyPrice().signum() == 0 ? "ACTIVE" : "PENDING_PAYMENT");
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusMonths(1));
        subscription.setAutoRenew(autoRenew);
        RecruiterSubscription saved = recruiterSubscriptionRepository.save(subscription);

        InvoiceRecord invoiceRecord = new InvoiceRecord();
        invoiceRecord.setRecruiterProfileId(recruiterProfileId);
        invoiceRecord.setPlanCode(plan.getCode());
        invoiceRecord.setAmount(plan.getMonthlyPrice());
        invoiceRecord.setStatus(plan.getMonthlyPrice().signum() == 0 ? "PAID" : "ISSUED");
        invoiceRecord.setDescription(plan.getName() + " monthly subscription");
        invoiceRecord.setDueDate(LocalDate.now().plusDays(7));
        invoiceRecordRepository.save(invoiceRecord);
        return saved;
    }

    public List<RecruiterSubscription> getAllSubscriptions() {
        return recruiterSubscriptionRepository.findAllByOrderByUpdatedAtDesc();
    }

    public List<InvoiceRecord> getInvoicesForRecruiter(Integer recruiterProfileId) {
        return invoiceRecordRepository.findByRecruiterProfileIdOrderByIssuedAtDesc(recruiterProfileId);
    }

    public List<InvoiceRecord> getAllInvoices() {
        return invoiceRecordRepository.findAllByOrderByIssuedAtDesc();
    }

    public SubscriptionPlan togglePlan(Long planId, boolean active) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
            .orElseThrow(() -> new IllegalArgumentException("Subscription plan not found with id: " + planId));
        plan.setActive(active);
        return subscriptionPlanRepository.save(plan);
    }

    private void createPlanIfMissing(String code, String name, BigDecimal monthlyPrice, String description) {
        if (subscriptionPlanRepository.findByCode(code).isPresent()) {
            return;
        }
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setCode(code);
        plan.setName(name);
        plan.setMonthlyPrice(monthlyPrice);
        plan.setDescription(description);
        plan.setActive(true);
        subscriptionPlanRepository.save(plan);
    }
}
