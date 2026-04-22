package com.hireconnect.web.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hireconnect.web.domain.SuspendedUser;
import com.hireconnect.web.dto.PortalSession;
import com.hireconnect.web.dto.ProfileResponse;
import com.hireconnect.web.repository.SuspendedUserRepository;

@Service
@Transactional
public class AdminSupportService {

    private final SuspendedUserRepository suspendedUserRepository;
    private final ProfileService profileService;

    public AdminSupportService(SuspendedUserRepository suspendedUserRepository, ProfileService profileService) {
        this.suspendedUserRepository = suspendedUserRepository;
        this.profileService = profileService;
    }

    public List<ProfileResponse> getAllUsers(PortalSession session) {
        return profileService.getAllProfiles(session);
    }

    public SuspendedUser suspendUser(Integer profileId, String reason, PortalSession session) {
        ProfileResponse profile = profileService.getAllProfiles(session).stream()
            .filter(candidate -> profileId.equals(candidate.profileId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Profile not found with id: " + profileId));

        SuspendedUser suspendedUser = new SuspendedUser();
        suspendedUser.setProfileId(profile.profileId());
        suspendedUser.setEmail(profile.email());
        suspendedUser.setRole(profile.role());
        suspendedUser.setReason(reason);
        suspendedUser.setActive(true);
        return suspendedUserRepository.save(suspendedUser);
    }

    public Map<Integer, SuspendedUser> getSuspendedUserMap() {
        Map<Integer, SuspendedUser> suspendedUserMap = new LinkedHashMap<>();
        for (SuspendedUser suspendedUser : suspendedUserRepository.findByActiveTrueOrderBySuspendedAtDesc()) {
            suspendedUserMap.put(suspendedUser.getProfileId(), suspendedUser);
        }
        return suspendedUserMap;
    }
}
