package com.hireconnect.web.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.hireconnect.web.dto.ApplicationResponse;
import com.hireconnect.web.dto.CandidateProfileForm;
import com.hireconnect.web.dto.AuthResponse;
import com.hireconnect.web.dto.InterviewResponse;
import com.hireconnect.web.dto.JobResponse;
import com.hireconnect.web.dto.LoginForm;
import com.hireconnect.web.dto.NotificationResponse;
import com.hireconnect.web.dto.PortalSession;
import com.hireconnect.web.dto.ProfileResponse;
import com.hireconnect.web.dto.RegisterForm;
import com.hireconnect.web.dto.WalletTopUpForm;
import com.hireconnect.web.service.ApplicationService;
import com.hireconnect.web.service.AuthService;
import com.hireconnect.web.service.CandidateFeatureService;
import com.hireconnect.web.service.InterviewService;
import com.hireconnect.web.service.JobService;
import com.hireconnect.web.service.NotificationService;
import com.hireconnect.web.service.ProfileService;
import com.hireconnect.web.support.PortalSessionService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping
public class CandidateController {

    private final AuthService authService;
    private final ProfileService profileService;
    private final JobService jobService;
    private final ApplicationService applicationService;
    private final InterviewService interviewService;
    private final NotificationService notificationService;
    private final CandidateFeatureService candidateFeatureService;
    private final PortalSessionService portalSessionService;

    public CandidateController(
        AuthService authService,
        ProfileService profileService,
        JobService jobService,
        ApplicationService applicationService,
        InterviewService interviewService,
        NotificationService notificationService,
        CandidateFeatureService candidateFeatureService,
        PortalSessionService portalSessionService
    ) {
        this.authService = authService;
        this.profileService = profileService;
        this.jobService = jobService;
        this.applicationService = applicationService;
        this.interviewService = interviewService;
        this.notificationService = notificationService;
        this.candidateFeatureService = candidateFeatureService;
        this.portalSessionService = portalSessionService;
    }

    @GetMapping({"/", "/candidate/home"})
    public String home(Model model, HttpSession session) {
        PortalSession portalSession = portalSessionService.getCurrentSession(session);
        if (portalSession != null) {
            return switch (portalSession.role()) {
                case "RECRUITER" -> "redirect:/recruiter/dashboard";
                case "ADMIN" -> "redirect:/admin/dashboard";
                default -> "redirect:/candidate/jobs";
            };
        }
        model.addAttribute("registerForm", new RegisterForm());
        model.addAttribute("loginForm", new LoginForm());
        return "home";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm registerForm, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "register";
        }
        AuthResponse authResponse = authService.register(registerForm);
        model.addAttribute("message", "Registration successful for " + authResponse.user().role() + ". Please log in.");
        model.addAttribute("loginForm", new LoginForm());
        return "login";
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("loginForm", new LoginForm());
        return "login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("loginForm") LoginForm loginForm, BindingResult bindingResult, HttpSession session, Model model) {
        if (bindingResult.hasErrors()) {
            return "login";
        }
        AuthResponse authResponse = authService.login(loginForm);
        portalSessionService.store(session, authService.toPortalSession(authResponse));
        return switch (authResponse.user().role()) {
            case "RECRUITER" -> "redirect:/recruiter/dashboard";
            case "ADMIN" -> "redirect:/admin/dashboard";
            default -> "redirect:/candidate/profile";
        };
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        portalSessionService.clear(session);
        return "redirect:/login";
    }

    @GetMapping("/candidate/profile")
    public ModelAndView viewProfile(HttpSession session) {
        PortalSession portalSession = portalSessionService.requireRole(session, "CANDIDATE");
        ModelAndView modelAndView = new ModelAndView("candidate/profile");
        ProfileResponse profile = findProfile(portalSession);
        CandidateProfileForm form = profile == null ? new CandidateProfileForm() : profileService.toCandidateForm(profile);
        if (profile == null) {
            form.setEmail(portalSession.email());
        }
        modelAndView.addObject("profileForm", form);
        modelAndView.addObject("profile", profile);
        modelAndView.addObject("sessionUser", portalSession);
        return modelAndView;
    }

    @PostMapping("/candidate/profile")
    public String saveProfile(
        @Valid @ModelAttribute("profileForm") CandidateProfileForm profileForm,
        BindingResult bindingResult,
        HttpSession session
    ) {
        if (bindingResult.hasErrors()) {
            return "candidate/profile";
        }
        PortalSession portalSession = portalSessionService.requireRole(session, "CANDIDATE");
        profileForm.setEmail(portalSession.email());
        profileService.saveCandidateProfile(profileForm, portalSession);
        return "redirect:/candidate/profile";
    }

    @GetMapping("/candidate/jobs")
    public ModelAndView searchJobs(
        @RequestParam(required = false) String title,
        @RequestParam(required = false) String location,
        @RequestParam(required = false) Double salaryMin,
        @RequestParam(required = false) Double salaryMax,
        HttpSession session
    ) {
        PortalSession portalSession = portalSessionService.requireRole(session, "CANDIDATE");
        ProfileResponse profile = requireProfile(portalSession);
        List<JobResponse> jobs = jobService.searchJobs(title, location, salaryMin, salaryMax, portalSession);
        ModelAndView modelAndView = new ModelAndView("candidate/jobs");
        modelAndView.addObject("jobs", jobs);
        modelAndView.addObject("searchTitle", title);
        modelAndView.addObject("searchLocation", location);
        modelAndView.addObject("salaryMin", salaryMin);
        modelAndView.addObject("salaryMax", salaryMax);
        modelAndView.addObject("profile", profile);
        modelAndView.addObject("bookmarkedJobIds", candidateFeatureService.getBookmarkedJobIds(profile.profileId()));
        modelAndView.addObject("walletBalance", candidateFeatureService.getWalletBalance(profile.profileId()));
        modelAndView.addObject("walletForm", new WalletTopUpForm());
        return modelAndView;
    }

    @PostMapping("/candidate/jobs/{jobId}/apply")
    public ModelAndView applyForJob(@PathVariable Integer jobId, HttpSession session) {
        PortalSession portalSession = portalSessionService.requireRole(session, "CANDIDATE");
        ProfileResponse profile = requireProfile(portalSession);
        applicationService.apply(jobId, profile.profileId(), profile.resumeUrl(), portalSession);
        return new ModelAndView("redirect:/candidate/applications");
    }

    @GetMapping("/candidate/applications")
    public ModelAndView viewApplications(HttpSession session) {
        PortalSession portalSession = portalSessionService.requireRole(session, "CANDIDATE");
        ProfileResponse profile = requireProfile(portalSession);
        ModelAndView modelAndView = new ModelAndView("candidate/applications");
        modelAndView.addObject("applications", applicationService.getByCandidate(profile.profileId(), portalSession));
        return modelAndView;
    }

    @GetMapping("/candidate/interviews")
    public ModelAndView viewInterviews(HttpSession session) {
        PortalSession portalSession = portalSessionService.requireRole(session, "CANDIDATE");
        ProfileResponse profile = requireProfile(portalSession);
        ModelAndView modelAndView = new ModelAndView("candidate/interviews");
        modelAndView.addObject("interviews", interviewService.getByCandidate(profile.profileId(), portalSession));
        return modelAndView;
    }

    @GetMapping("/candidate/notifications")
    public ModelAndView viewNotifications(HttpSession session) {
        PortalSession portalSession = portalSessionService.requireRole(session, "CANDIDATE");
        ProfileResponse profile = requireProfile(portalSession);
        ModelAndView modelAndView = new ModelAndView("candidate/notifications");
        modelAndView.addObject("notifications", notificationService.getByUser(profile.profileId(), portalSession));
        modelAndView.addObject("unreadCount", notificationService.getUnreadCount(profile.profileId(), portalSession));
        return modelAndView;
    }

    @PostMapping("/candidate/jobs/{jobId}/bookmark")
    public ModelAndView bookmarkJob(@PathVariable Integer jobId, HttpSession session) {
        PortalSession portalSession = portalSessionService.requireRole(session, "CANDIDATE");
        ProfileResponse profile = requireProfile(portalSession);
        candidateFeatureService.bookmarkJob(profile.profileId(), jobId);
        return new ModelAndView("redirect:/candidate/jobs");
    }

    @PostMapping("/candidate/wallet")
    public ModelAndView addMoneyToWallet(@Valid @ModelAttribute("walletForm") WalletTopUpForm walletTopUpForm, HttpSession session) {
        PortalSession portalSession = portalSessionService.requireRole(session, "CANDIDATE");
        ProfileResponse profile = requireProfile(portalSession);
        candidateFeatureService.addMoneyToWallet(profile.profileId(), walletTopUpForm.getAmount());
        return new ModelAndView("redirect:/candidate/jobs");
    }

    private ProfileResponse findProfile(PortalSession portalSession) {
        try {
            return profileService.getProfileByEmail(portalSession.email(), portalSession);
        } catch (Exception ex) {
            return null;
        }
    }

    private ProfileResponse requireProfile(PortalSession portalSession) {
        ProfileResponse profile = findProfile(portalSession);
        if (profile == null) {
            throw new com.hireconnect.web.support.PortalException(org.springframework.http.HttpStatus.BAD_REQUEST, "Please complete your candidate profile first");
        }
        return profile;
    }
}
