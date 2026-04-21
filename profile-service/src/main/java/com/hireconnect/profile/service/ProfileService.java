package com.hireconnect.profile.service;

import java.util.List;
import java.util.Map;

import com.hireconnect.profile.dto.CandidateProfileRequest;
import com.hireconnect.profile.dto.ProfileResponse;
import com.hireconnect.profile.dto.RecruiterProfileRequest;

public interface ProfileService {

    ProfileResponse addCandidateProfile(CandidateProfileRequest request);

    ProfileResponse addRecruiterProfile(RecruiterProfileRequest request);

    ProfileResponse updateProfile(Integer profileId, Map<String, Object> updates);

    void deleteProfile(Integer profileId);

    ProfileResponse getProfileById(Integer profileId);

    ProfileResponse getByEmail(String email);

    ProfileResponse getByMobile(Long mobile);

    List<ProfileResponse> getAllProfiles();

    List<ProfileResponse> getAllProfilesByRole(String role);
}
