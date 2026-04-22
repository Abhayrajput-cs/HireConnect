package com.hireconnect.web.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.hireconnect.web.dto.InterviewScheduleForm;
import com.hireconnect.web.dto.JobForm;
import com.hireconnect.web.dto.PortalSession;
import com.hireconnect.web.dto.ProfileResponse;
import com.hireconnect.web.dto.RecruiterProfileForm;
import com.hireconnect.web.dto.SubscriptionSelectionForm;
import com.hireconnect.web.service.AnalyticsService;
import com.hireconnect.web.service.ApplicationService;
import com.hireconnect.web.service.InterviewService;
import com.hireconnect.web.service.JobService;
import com.hireconnect.web.service.ProfileService;
import com.hireconnect.web.service.SubscriptionService;
import com.hireconnect.web.support.PortalException;
import com.hireconnect.web.support.PortalSessionService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/recruiter")
public class RecruiterController {

    private final ProfileService profileService;
    private final JobService jobService;
    private final ApplicationService applicationService;
    private final InterviewService interviewService;
    private final AnalyticsService analyticsService;
    private final SubscriptionService subscriptionService;
    private final PortalSessionService portalSessionService;

    public RecruiterController(
        ProfileService profileService,
        JobService jobService,
        ApplicationService applicationService,
        InterviewService interviewService,
        AnalyticsService analyticsService,
        SubscriptionService subscriptionService,
        PortalSessionService portalSessionService
    ) {
        this.profileService = profileService;
        this.jobService = jobService;
        this.applicationService = applicationService;
        this.interviewService = interviewService;
        this.analyticsService = analyticsService;
        this.subscriptionService = subscriptionService;
        this.portalSessionService = portalSessionService;
    }

    @GetMapping("/dashboard")
    public ModelAndView recruiterDashboard(HttpSession session) {
        PortalSession portalSession = portalSessionService.requireRole(session, "RECRUITER");
        ProfileResponse profile = findRecruiterProfile(portalSession);
        ModelAndView modelAndView = new ModelAndView("recruiter/dashboard");
        modelAndView.addObject("sessionUser", portalSession);
        modelAndView.addObject("profile", profile);
        modelAndView.addObject("profileForm", profile == null ? defaultRecruiterForm(portalSession) : profileService.toRecruiterForm(profile));
        modelAndView.addObject("jobs", profile == null ? List.of() : jobService.getJobsByRecruiter(profile.profileId(), portalSession));
        modelAndView.addObject("currentSubscription", profile == null ? null : subscriptionService.getCurrentSubscription(profile.profileId()));
        return modelAndView;
    }

    @PostMapping("/profile")
    public Object saveRecruiterProfile(
        @Valid @ModelAttribute("profileForm") RecruiterProfileForm profileForm,
        BindingResult bindingResult,
        HttpSession session
    ) {
        PortalSession portalSession = portalSessionService.requireRole(session, "RECRUITER");
        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = recruiterDashboard(session);
            modelAndView.addObject("profileForm", profileForm);
            return modelAndView;
        }
        profileForm.setEmail(portalSession.email());
        profileService.saveRecruiterProfile(profileForm, portalSession);
        return "redirect:/recruiter/dashboard";
    }

    @GetMapping("/jobs/new")
    public ModelAndView newJob(HttpSession session) {
        portalSessionService.requireRole(session, "RECRUITER");
        return jobFormView(new JobForm(), "/recruiter/jobs", "Post Job");
    }

    @PostMapping("/jobs")
    public Object postJob(@Valid @ModelAttribute("jobForm") JobForm jobForm, BindingResult bindingResult, HttpSession session) {
        PortalSession portalSession = portalSessionService.requireRole(session, "RECRUITER");
        if (bindingResult.hasErrors()) {
            return jobFormView(jobForm, "/recruiter/jobs", "Post Job");
        }
        ProfileResponse profile = requireRecruiterProfile(portalSession);
        jobService.createJob(jobForm, profile.profileId(), portalSession);
        return "redirect:/recruiter/dashboard";
    }

    @GetMapping("/jobs/{jobId}/edit")
    public ModelAndView editJob(@PathVariable Integer jobId, HttpSession session) {
        PortalSession portalSession = portalSessionService.requireRole(session, "RECRUITER");
        return jobFormView(jobService.toForm(jobService.getJob(jobId, portalSession)), "/recruiter/jobs/" + jobId, "Edit Job");
    }

    @PostMapping("/jobs/{jobId}")
    public Object updateJob(
        @PathVariable Integer jobId,
        @Valid @ModelAttribute("jobForm") JobForm jobForm,
        BindingResult bindingResult,
        HttpSession session
    ) {
        PortalSession portalSession = portalSessionService.requireRole(session, "RECRUITER");
        if (bindingResult.hasErrors()) {
            return jobFormView(jobForm, "/recruiter/jobs/" + jobId, "Edit Job");
        }
        jobService.updateJob(jobId, jobForm, portalSession);
        return "redirect:/recruiter/dashboard";
    }

    @GetMapping("/jobs/{jobId}/applications")
    public ModelAndView viewApplications(@PathVariable Integer jobId, HttpSession session) {
        PortalSession portalSession = portalSessionService.requireRole(session, "RECRUITER");
        return applicationsView(jobId, new InterviewScheduleForm(), portalSession);
    }

    @PostMapping("/applications/{applicationId}/shortlist")
    public String shortlistCandidate(@PathVariable Integer applicationId, HttpSession session) {
        PortalSession portalSession = portalSessionService.requireRole(session, "RECRUITER");
        Integer jobId = applicationService.shortlistCandidate(applicationId, portalSession).jobId();
        return "redirect:/recruiter/jobs/" + jobId + "/applications";
    }

    @PostMapping("/applications/{applicationId}/reject")
    public String rejectCandidate(@PathVariable Integer applicationId, HttpSession session) {
        PortalSession portalSession = portalSessionService.requireRole(session, "RECRUITER");
        Integer jobId = applicationService.rejectCandidate(applicationId, portalSession).jobId();
        return "redirect:/recruiter/jobs/" + jobId + "/applications";
    }

    @PostMapping("/interviews")
    public Object scheduleInterview(
        @Valid @ModelAttribute("scheduleForm") InterviewScheduleForm scheduleForm,
        BindingResult bindingResult,
        HttpSession session
    ) {
        PortalSession portalSession = portalSessionService.requireRole(session, "RECRUITER");
        if (bindingResult.hasErrors()) {
            Integer jobId = applicationService.getById(scheduleForm.getApplicationId(), portalSession).jobId();
            return applicationsView(jobId, scheduleForm, portalSession);
        }
        interviewService.scheduleInterview(scheduleForm, portalSession);
        Integer jobId = applicationService.getById(scheduleForm.getApplicationId(), portalSession).jobId();
        return "redirect:/recruiter/jobs/" + jobId + "/applications";
    }

    @GetMapping("/analytics")
    public ModelAndView viewAnalytics(HttpSession session) {
        PortalSession portalSession = portalSessionService.requireRole(session, "RECRUITER");
        ProfileResponse profile = requireRecruiterProfile(portalSession);
        ModelAndView modelAndView = new ModelAndView("recruiter/analytics");
        modelAndView.addObject("summary", analyticsService.getRecruiterStats(profile.profileId(), portalSession));
        modelAndView.addObject("timeToHire", analyticsService.getTimeToHire(profile.profileId(), portalSession));
        modelAndView.addObject("topCategories", analyticsService.getTopJobCategories(portalSession));
        return modelAndView;
    }

    @GetMapping("/subscription")
    public ModelAndView managePlan(HttpSession session) {
        PortalSession portalSession = portalSessionService.requireRole(session, "RECRUITER");
        ProfileResponse profile = requireRecruiterProfile(portalSession);
        ModelAndView modelAndView = new ModelAndView("recruiter/subscription");
        modelAndView.addObject("plans", subscriptionService.getActivePlans());
        modelAndView.addObject("currentSubscription", subscriptionService.getCurrentSubscription(profile.profileId()));
        modelAndView.addObject("subscriptionForm", new SubscriptionSelectionForm());
        return modelAndView;
    }

    @PostMapping("/subscription")
    public String savePlan(@Valid @ModelAttribute("subscriptionForm") SubscriptionSelectionForm subscriptionForm, HttpSession session) {
        PortalSession portalSession = portalSessionService.requireRole(session, "RECRUITER");
        ProfileResponse profile = requireRecruiterProfile(portalSession);
        subscriptionService.subscribe(profile.profileId(), subscriptionForm.getPlanCode(), subscriptionForm.isAutoRenew());
        return "redirect:/recruiter/invoices";
    }

    @GetMapping("/invoices")
    public ModelAndView viewInvoices(HttpSession session) {
        PortalSession portalSession = portalSessionService.requireRole(session, "RECRUITER");
        ProfileResponse profile = requireRecruiterProfile(portalSession);
        ModelAndView modelAndView = new ModelAndView("recruiter/invoices");
        modelAndView.addObject("invoices", subscriptionService.getInvoicesForRecruiter(profile.profileId()));
        return modelAndView;
    }

    private ModelAndView jobFormView(JobForm jobForm, String formAction, String pageTitle) {
        ModelAndView modelAndView = new ModelAndView("recruiter/job-form");
        modelAndView.addObject("jobForm", jobForm);
        modelAndView.addObject("formAction", formAction);
        modelAndView.addObject("pageTitle", pageTitle);
        return modelAndView;
    }

    private ModelAndView applicationsView(Integer jobId, InterviewScheduleForm scheduleForm, PortalSession portalSession) {
        ModelAndView modelAndView = new ModelAndView("recruiter/applications");
        modelAndView.addObject("applications", applicationService.getByJob(jobId, portalSession));
        modelAndView.addObject("job", jobService.getJob(jobId, portalSession));
        modelAndView.addObject("scheduleForm", scheduleForm);
        return modelAndView;
    }

    private ProfileResponse findRecruiterProfile(PortalSession portalSession) {
        try {
            return profileService.getProfileByEmail(portalSession.email(), portalSession);
        } catch (Exception ex) {
            return null;
        }
    }

    private ProfileResponse requireRecruiterProfile(PortalSession portalSession) {
        ProfileResponse profile = findRecruiterProfile(portalSession);
        if (profile == null) {
            throw new PortalException(org.springframework.http.HttpStatus.BAD_REQUEST, "Please complete your recruiter profile first");
        }
        return profile;
    }

    private RecruiterProfileForm defaultRecruiterForm(PortalSession portalSession) {
        RecruiterProfileForm form = new RecruiterProfileForm();
        form.setEmail(portalSession.email());
        return form;
    }
}
