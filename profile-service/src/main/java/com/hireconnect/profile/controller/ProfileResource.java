package com.hireconnect.profile.controller;

import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.hireconnect.profile.dto.CandidateProfileRequest;
import com.hireconnect.profile.dto.ProfileResponse;
import com.hireconnect.profile.dto.RecruiterProfileRequest;
import com.hireconnect.profile.dto.ResumeUploadResponse;
import com.hireconnect.profile.service.ProfileService;
import com.hireconnect.profile.service.ResumeStorageService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileResource {

    private final ProfileService profileService;
    private final ResumeStorageService resumeStorageService;

    public ProfileResource(ProfileService profileService, ResumeStorageService resumeStorageService) {
        this.profileService = profileService;
        this.resumeStorageService = resumeStorageService;
    }

    @PostMapping("/candidates")
    public ResponseEntity<ProfileResponse> addCandidate(@Valid @RequestBody CandidateProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profileService.addCandidateProfile(request));
    }

    @PostMapping("/recruiters")
    public ResponseEntity<ProfileResponse> addRecruiter(@Valid @RequestBody RecruiterProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profileService.addRecruiterProfile(request));
    }

    @PostMapping(value = "/resumes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeUploadResponse> uploadResume(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resumeStorageService.uploadResume(file));
    }

    @GetMapping("/resumes/{fileName:.+}")
    public ResponseEntity<Resource> getResume(@PathVariable String fileName) {
        Resource resource = resumeStorageService.loadResume(fileName);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(resumeStorageService.determineContentType(fileName)))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
            .body(resource);
    }

    @GetMapping
    public ResponseEntity<List<ProfileResponse>> getAll(@RequestParam(required = false) String role) {
        if (role == null || role.isBlank()) {
            return ResponseEntity.ok(profileService.getAllProfiles());
        }
        return ResponseEntity.ok(profileService.getAllProfilesByRole(role));
    }

    @GetMapping("/{profileId}")
    public ResponseEntity<ProfileResponse> getById(@PathVariable Integer profileId) {
        return ResponseEntity.ok(profileService.getProfileById(profileId));
    }

    @GetMapping("/email/{email:.+}")
    public ResponseEntity<ProfileResponse> getByEmail(@PathVariable String email) {
        return ResponseEntity.ok(profileService.getByEmail(email));
    }

    @GetMapping("/mobile/{mobile}")
    public ResponseEntity<ProfileResponse> getByMobile(@PathVariable Long mobile) {
        return ResponseEntity.ok(profileService.getByMobile(mobile));
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<List<ProfileResponse>> getByRole(@PathVariable String role) {
        return ResponseEntity.ok(profileService.getAllProfilesByRole(role));
    }

    @PutMapping("/{profileId}")
    public ResponseEntity<ProfileResponse> updateProfile(
        @PathVariable Integer profileId,
        @RequestBody Map<String, Object> updates
    ) {
        return ResponseEntity.ok(profileService.updateProfile(profileId, updates));
    }

    @DeleteMapping("/{profileId}")
    public ResponseEntity<Void> deleteProfile(@PathVariable Integer profileId) {
        profileService.deleteProfile(profileId);
        return ResponseEntity.noContent().build();
    }
}
