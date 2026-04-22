package com.hireconnect.web.controller;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.hireconnect.web.dto.PortalSession;
import com.hireconnect.web.dto.SuspendUserForm;
import com.hireconnect.web.service.AdminSupportService;
import com.hireconnect.web.service.AnalyticsService;
import com.hireconnect.web.service.JobService;
import com.hireconnect.web.service.SubscriptionService;
import com.hireconnect.web.support.PortalSessionService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminSupportService adminSupportService;
    private final AnalyticsService analyticsService;
    private final SubscriptionService subscriptionService;
    private final JobService jobService;
    private final PortalSessionService portalSessionService;

    public AdminController(
        AdminSupportService adminSupportService,
        AnalyticsService analyticsService,
        SubscriptionService subscriptionService,
        JobService jobService,
        PortalSessionService portalSessionService
    ) {
        this.adminSupportService = adminSupportService;
        this.analyticsService = analyticsService;
        this.subscriptionService = subscriptionService;
        this.jobService = jobService;
        this.portalSessionService = portalSessionService;
    }

    @GetMapping("/dashboard")
    public ModelAndView adminDashboard(HttpSession session) {
        PortalSession portalSession = portalSessionService.requireRole(session, "ADMIN");
        ModelAndView modelAndView = new ModelAndView("admin/dashboard");
        modelAndView.addObject("sessionUser", portalSession);
        modelAndView.addObject("platformSummary", analyticsService.getPlatformStats(portalSession));
        modelAndView.addObject("activeSubscriptions", subscriptionService.getAllSubscriptions().size());
        modelAndView.addObject("invoiceCount", subscriptionService.getAllInvoices().size());
        return modelAndView;
    }

    @GetMapping("/users")
    public ModelAndView manageUsers(HttpSession session) {
        PortalSession portalSession = portalSessionService.requireRole(session, "ADMIN");
        ModelAndView modelAndView = new ModelAndView("admin/users");
        modelAndView.addObject("users", adminSupportService.getAllUsers(portalSession));
        modelAndView.addObject("suspendedUsers", adminSupportService.getSuspendedUserMap());
        modelAndView.addObject("suspendForm", new SuspendUserForm());
        return modelAndView;
    }

    @PostMapping("/users/{profileId}/suspend")
    public Object suspendUser(
        @PathVariable Integer profileId,
        @Valid @ModelAttribute("suspendForm") SuspendUserForm suspendUserForm,
        BindingResult bindingResult,
        HttpSession session
    ) {
        PortalSession portalSession = portalSessionService.requireRole(session, "ADMIN");
        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = manageUsers(session);
            modelAndView.addObject("suspendForm", suspendUserForm);
            return modelAndView;
        }
        adminSupportService.suspendUser(profileId, suspendUserForm.getReason(), portalSession);
        return "redirect:/admin/users";
    }

    @GetMapping("/jobs")
    public ModelAndView viewAllJobs(HttpSession session) {
        PortalSession portalSession = portalSessionService.requireRole(session, "ADMIN");
        ModelAndView modelAndView = new ModelAndView("admin/jobs");
        modelAndView.addObject("jobs", jobService.getAllJobs(portalSession));
        return modelAndView;
    }

    @PostMapping("/jobs/{jobId}/delete")
    public String deleteJob(@PathVariable Integer jobId, HttpSession session) {
        PortalSession portalSession = portalSessionService.requireRole(session, "ADMIN");
        jobService.deleteJob(jobId, portalSession);
        return "redirect:/admin/jobs";
    }

    @GetMapping("/analytics")
    public ModelAndView viewPlatformAnalytics(HttpSession session) {
        PortalSession portalSession = portalSessionService.requireRole(session, "ADMIN");
        ModelAndView modelAndView = new ModelAndView("admin/analytics");
        modelAndView.addObject("platformSummary", analyticsService.getPlatformStats(portalSession));
        modelAndView.addObject("topCategories", analyticsService.getTopJobCategories(portalSession));
        return modelAndView;
    }

    @GetMapping("/subscriptions")
    public ModelAndView manageSubscriptions(HttpSession session) {
        portalSessionService.requireRole(session, "ADMIN");
        ModelAndView modelAndView = new ModelAndView("admin/subscriptions");
        modelAndView.addObject("plans", subscriptionService.getAllPlans());
        modelAndView.addObject("subscriptions", subscriptionService.getAllSubscriptions());
        return modelAndView;
    }

    @PostMapping("/subscriptions/{planId}/toggle")
    public String togglePlan(@PathVariable Long planId, @RequestParam boolean active, HttpSession session) {
        portalSessionService.requireRole(session, "ADMIN");
        subscriptionService.togglePlan(planId, active);
        return "redirect:/admin/subscriptions";
    }

    @GetMapping("/invoices")
    public ModelAndView viewAllInvoices(HttpSession session) {
        portalSessionService.requireRole(session, "ADMIN");
        ModelAndView modelAndView = new ModelAndView("admin/invoices");
        modelAndView.addObject("invoices", subscriptionService.getAllInvoices());
        return modelAndView;
    }

    @GetMapping("/reports/export")
    public ResponseEntity<byte[]> exportReport(HttpSession session) {
        PortalSession portalSession = portalSessionService.requireRole(session, "ADMIN");
        StringBuilder csv = new StringBuilder();
        csv.append("metric,value\n");
        var summary = analyticsService.getPlatformStats(portalSession);
        csv.append("totalJobs,").append(summary.getTotalJobs()).append('\n');
        csv.append("totalApplications,").append(summary.getTotalApplications()).append('\n');
        csv.append("shortlistedCount,").append(summary.getShortlistedCount()).append('\n');
        csv.append("offeredCount,").append(summary.getOfferedCount()).append('\n');
        csv.append("rejectedCount,").append(summary.getRejectedCount()).append('\n');
        csv.append("avgTimeToHireDays,").append(summary.getAvgTimeToHireDays()).append('\n');
        csv.append("viewToApplyRatio,").append(summary.getViewToApplyRatio()).append('\n');
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=hireconnect-platform-report.csv")
            .contentType(MediaType.TEXT_PLAIN)
            .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }
}
