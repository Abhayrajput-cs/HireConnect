package com.hireconnect.web.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

import com.hireconnect.web.dto.AddressResponse;
import com.hireconnect.web.dto.CandidateProfileForm;
import com.hireconnect.web.dto.PortalSession;
import com.hireconnect.web.dto.ProfileResponse;
import com.hireconnect.web.dto.RecruiterProfileForm;
import com.hireconnect.web.support.GatewayClient;

@Service
public class ProfileService {

    private final GatewayClient gatewayClient;

    public ProfileService(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    public ProfileResponse getProfileByEmail(String email, PortalSession session) {
        String encodedEmail = UriUtils.encodePathSegment(email, java.nio.charset.StandardCharsets.UTF_8);
        return gatewayClient.get("/api/v1/profiles/email/" + encodedEmail, session, ProfileResponse.class);
    }

    public List<ProfileResponse> getAllProfiles(PortalSession session) {
        return gatewayClient.get("/api/v1/profiles", session, new ParameterizedTypeReference<>() {
        });
    }

    public List<ProfileResponse> getProfilesByRole(String role, PortalSession session) {
        String normalizedRole = UriUtils.encodePathSegment(role, java.nio.charset.StandardCharsets.UTF_8);
        return gatewayClient.get("/api/v1/profiles/role/" + normalizedRole, session, new ParameterizedTypeReference<>() {
        });
    }

    public ProfileResponse saveCandidateProfile(CandidateProfileForm form, PortalSession session) {
        ProfileResponse existing = findProfileByEmail(form.getEmail(), session);
        Map<String, Object> payload = Map.of(
            "fullName", form.getFullName(),
            "email", form.getEmail(),
            "mobile", form.getMobile(),
            "dob", parseDate(form.getDob()),
            "gender", form.getGender(),
            "skills", splitCsv(form.getSkills()),
            "experience", form.getExperience(),
            "resumeUrl", form.getResumeUrl(),
            "addresses", List.of(Map.of(
                "houseNo", nullSafe(form.getHouseNo()),
                "street", nullSafe(form.getStreet()),
                "city", nullSafe(form.getCity()),
                "state", nullSafe(form.getState()),
                "pincode", form.getPincode()
            ))
        );
        if (existing == null) {
            return gatewayClient.post("/api/v1/profiles/candidates", session, payload, ProfileResponse.class);
        }
        return gatewayClient.put("/api/v1/profiles/{profileId}", session, payload, ProfileResponse.class, existing.profileId());
    }

    public ProfileResponse saveRecruiterProfile(RecruiterProfileForm form, PortalSession session) {
        ProfileResponse existing = findProfileByEmail(form.getEmail(), session);
        Map<String, Object> payload = Map.of(
            "fullName", form.getFullName(),
            "email", form.getEmail(),
            "mobile", form.getMobile(),
            "companyName", form.getCompanyName(),
            "companySize", nullSafe(form.getCompanySize()),
            "industry", form.getIndustry(),
            "website", nullSafe(form.getWebsite()),
            "addresses", List.of(Map.of(
                "houseNo", nullSafe(form.getHouseNo()),
                "street", nullSafe(form.getStreet()),
                "city", nullSafe(form.getCity()),
                "state", nullSafe(form.getState()),
                "pincode", form.getPincode()
            ))
        );
        if (existing == null) {
            return gatewayClient.post("/api/v1/profiles/recruiters", session, payload, ProfileResponse.class);
        }
        return gatewayClient.put("/api/v1/profiles/{profileId}", session, payload, ProfileResponse.class, existing.profileId());
    }

    public CandidateProfileForm toCandidateForm(ProfileResponse profile) {
        CandidateProfileForm form = new CandidateProfileForm();
        form.setFullName(profile.fullName());
        form.setEmail(profile.email());
        form.setMobile(profile.mobile());
        form.setDob(profile.dob() == null ? null : profile.dob().toString());
        form.setGender(profile.gender());
        form.setSkills(profile.skills() == null ? "" : String.join(", ", profile.skills()));
        form.setExperience(profile.experience());
        form.setResumeUrl(profile.resumeUrl());
        applyAddress(form, profile.addresses());
        return form;
    }

    public RecruiterProfileForm toRecruiterForm(ProfileResponse profile) {
        RecruiterProfileForm form = new RecruiterProfileForm();
        form.setFullName(profile.fullName());
        form.setEmail(profile.email());
        form.setMobile(profile.mobile());
        form.setCompanyName(profile.companyName());
        form.setCompanySize(profile.companySize());
        form.setIndustry(profile.industry());
        form.setWebsite(profile.website());
        applyAddress(form, profile.addresses());
        return form;
    }

    private ProfileResponse findProfileByEmail(String email, PortalSession session) {
        try {
            return getProfileByEmail(email, session);
        } catch (Exception ex) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        return StringUtils.hasText(value) ? LocalDate.parse(value) : null;
    }

    private List<String> splitCsv(String value) {
        return Stream.of(value.split(","))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .toList();
    }

    private String nullSafe(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private void applyAddress(CandidateProfileForm form, List<AddressResponse> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return;
        }
        AddressResponse address = addresses.getFirst();
        form.setHouseNo(address.houseNo());
        form.setStreet(address.street());
        form.setCity(address.city());
        form.setState(address.state());
        form.setPincode(address.pincode());
    }

    private void applyAddress(RecruiterProfileForm form, List<AddressResponse> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return;
        }
        AddressResponse address = addresses.getFirst();
        form.setHouseNo(address.houseNo());
        form.setStreet(address.street());
        form.setCity(address.city());
        form.setState(address.state());
        form.setPincode(address.pincode());
    }
}
