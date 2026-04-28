package com.hireconnect.profile.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireconnect.profile.domain.Address;
import com.hireconnect.profile.domain.CandidateProfile;
import com.hireconnect.profile.domain.RecruiterProfile;
import com.hireconnect.profile.domain.UserProfile;
import com.hireconnect.profile.dto.AddressRequest;
import com.hireconnect.profile.dto.AddressResponse;
import com.hireconnect.profile.dto.CandidateProfileRequest;
import com.hireconnect.profile.dto.ProfileResponse;
import com.hireconnect.profile.dto.RecruiterProfileRequest;
import com.hireconnect.profile.exception.ApiException;
import com.hireconnect.profile.repository.ProfileRepository;

@Service
@Transactional
public class ProfileServiceImpl implements ProfileService {

    private static final String CANDIDATE = "CANDIDATE";
    private static final String RECRUITER = "RECRUITER";

    private final ProfileRepository profileRepository;
    private final ObjectMapper objectMapper;

    public ProfileServiceImpl(ProfileRepository profileRepository, ObjectMapper objectMapper) {
        this.profileRepository = profileRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @CacheEvict(
        cacheNames = {"profileById", "profileByEmail", "profileByMobile", "profilesAll", "profilesByRole"},
        allEntries = true
    )
    public ProfileResponse addCandidateProfile(CandidateProfileRequest request) {
        String email = normalizeEmail(request.email());
        ensureEmailAvailable(email, null);
        ensureMobileAvailable(request.mobile(), null);

        CandidateProfile candidateProfile = new CandidateProfile();
        candidateProfile.setRole(CANDIDATE);
        candidateProfile.setFullName(request.fullName().trim());
        candidateProfile.setEmail(email);
        candidateProfile.setMobile(request.mobile());
        candidateProfile.setDob(request.dob());
        candidateProfile.setGender(normalizeOptionalText(request.gender()));
        candidateProfile.setSkills(normalizeSkills(request.skills()));
        candidateProfile.setExperience(request.experience());
        candidateProfile.setResumeUrl(request.resumeUrl().trim());
        candidateProfile.replaceAddresses(toAddressEntities(request.addresses()));

        return toResponse(profileRepository.save(candidateProfile));
    }

    @Override
    @CacheEvict(
        cacheNames = {"profileById", "profileByEmail", "profileByMobile", "profilesAll", "profilesByRole"},
        allEntries = true
    )
    public ProfileResponse addRecruiterProfile(RecruiterProfileRequest request) {
        String email = normalizeEmail(request.email());
        ensureEmailAvailable(email, null);
        ensureMobileAvailable(request.mobile(), null);

        RecruiterProfile recruiterProfile = new RecruiterProfile();
        recruiterProfile.setRole(RECRUITER);
        recruiterProfile.setFullName(request.fullName().trim());
        recruiterProfile.setEmail(email);
        recruiterProfile.setMobile(request.mobile());
        recruiterProfile.setDob(request.dob());
        recruiterProfile.setGender(normalizeOptionalText(request.gender()));
        recruiterProfile.setCompanyName(request.companyName().trim());
        recruiterProfile.setCompanySize(normalizeOptionalText(request.companySize()));
        recruiterProfile.setIndustry(request.industry().trim());
        recruiterProfile.setWebsite(normalizeOptionalText(request.website()));
        recruiterProfile.replaceAddresses(toAddressEntities(request.addresses()));

        return toResponse(profileRepository.save(recruiterProfile));
    }

    @Override
    @CacheEvict(
        cacheNames = {"profileById", "profileByEmail", "profileByMobile", "profilesAll", "profilesByRole"},
        allEntries = true
    )
    public ProfileResponse updateProfile(Integer profileId, Map<String, Object> updates) {
        UserProfile profile = loadProfile(profileId);

        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            applyUpdate(profile, entry.getKey(), entry.getValue());
        }

        return toResponse(profileRepository.save(profile));
    }

    @Override
    @CacheEvict(
        cacheNames = {"profileById", "profileByEmail", "profileByMobile", "profilesAll", "profilesByRole"},
        allEntries = true
    )
    public void deleteProfile(Integer profileId) {
        UserProfile profile = loadProfile(profileId);
        profileRepository.delete(profile);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "profileById", key = "#profileId")
    public ProfileResponse getProfileById(Integer profileId) {
        return toResponse(loadProfile(profileId));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "profileByEmail", key = "#email.trim().toLowerCase()")
    public ProfileResponse getByEmail(String email) {
        return toResponse(profileRepository.findByEmail(normalizeEmail(email))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profile not found with email: " + email)));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "profileByMobile", key = "#mobile")
    public ProfileResponse getByMobile(Long mobile) {
        return toResponse(profileRepository.findByMobile(mobile)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profile not found with mobile: " + mobile)));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "profilesAll")
    public List<ProfileResponse> getAllProfiles() {
        return profileRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "profilesByRole", key = "#role.trim().toUpperCase()")
    public List<ProfileResponse> getAllProfilesByRole(String role) {
        String normalizedRole = normalizeRole(role);
        return profileRepository.findAllByRole(normalizedRole).stream()
            .map(this::toResponse)
            .toList();
    }

    private UserProfile loadProfile(Integer profileId) {
        return profileRepository.findByProfileId(profileId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profile not found with id: " + profileId));
    }

    private void applyUpdate(UserProfile profile, String field, Object rawValue) {
        switch (field) {
            case "fullName" -> profile.setFullName(asTrimmedText(rawValue, "fullName"));
            case "email" -> {
                String email = normalizeEmail(asTrimmedText(rawValue, "email"));
                ensureEmailAvailable(email, profile.getProfileId());
                profile.setEmail(email);
            }
            case "mobile" -> {
                Long mobile = objectMapper.convertValue(rawValue, Long.class);
                ensureMobileAvailable(mobile, profile.getProfileId());
                profile.setMobile(mobile);
            }
            case "dob" -> profile.setDob(objectMapper.convertValue(rawValue, LocalDate.class));
            case "gender" -> profile.setGender(normalizeOptionalText(objectMapper.convertValue(rawValue, String.class)));
            case "addresses" -> {
                List<AddressRequest> addressRequests = objectMapper.convertValue(
                    rawValue,
                    new TypeReference<List<AddressRequest>>() {
                    }
                );
                profile.replaceAddresses(toAddressEntities(addressRequests));
            }
            case "skills" -> updateCandidate(profile).setSkills(normalizeSkills(objectMapper.convertValue(
                rawValue,
                new TypeReference<List<String>>() {
                }
            )));
            case "experience" -> updateCandidate(profile).setExperience(objectMapper.convertValue(rawValue, Integer.class));
            case "resumeUrl" -> updateCandidate(profile).setResumeUrl(asTrimmedText(rawValue, "resumeUrl"));
            case "companyName" -> updateRecruiter(profile).setCompanyName(asTrimmedText(rawValue, "companyName"));
            case "companySize" -> updateRecruiter(profile).setCompanySize(normalizeOptionalText(
                objectMapper.convertValue(rawValue, String.class)
            ));
            case "industry" -> updateRecruiter(profile).setIndustry(asTrimmedText(rawValue, "industry"));
            case "website" -> updateRecruiter(profile).setWebsite(normalizeOptionalText(
                objectMapper.convertValue(rawValue, String.class)
            ));
            case "profileId", "role" -> throw new ApiException(HttpStatus.BAD_REQUEST, "Field cannot be updated: " + field);
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported update field: " + field);
        }
    }

    private CandidateProfile updateCandidate(UserProfile profile) {
        if (profile instanceof CandidateProfile candidateProfile) {
            return candidateProfile;
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "Candidate-only field provided for a recruiter profile");
    }

    private RecruiterProfile updateRecruiter(UserProfile profile) {
        if (profile instanceof RecruiterProfile recruiterProfile) {
            return recruiterProfile;
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "Recruiter-only field provided for a candidate profile");
    }

    private void ensureEmailAvailable(String email, Integer currentProfileId) {
        profileRepository.findByEmail(email)
            .filter(existing -> !existing.getProfileId().equals(currentProfileId))
            .ifPresent(existing -> {
                throw new ApiException(HttpStatus.CONFLICT, "Email is already associated with another profile");
            });
    }

    private void ensureMobileAvailable(Long mobile, Integer currentProfileId) {
        if (mobile == null) {
            return;
        }

        profileRepository.findByMobile(mobile)
            .filter(existing -> !existing.getProfileId().equals(currentProfileId))
            .ifPresent(existing -> {
                throw new ApiException(HttpStatus.CONFLICT, "Mobile is already associated with another profile");
            });
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Role is required");
        }

        String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
        if (!CANDIDATE.equals(normalizedRole) && !RECRUITER.equals(normalizedRole)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Role must be either CANDIDATE or RECRUITER");
        }
        return normalizedRole;
    }

    private String asTrimmedText(Object rawValue, String field) {
        String value = objectMapper.convertValue(rawValue, String.class);
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Field is required: " + field);
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private List<String> normalizeSkills(List<String> skills) {
        List<String> normalizedSkills = new ArrayList<>();
        if (skills == null || skills.isEmpty()) {
            return normalizedSkills;
        }

        for (String skill : skills) {
            if (skill == null || skill.isBlank()) {
                continue;
            }
            normalizedSkills.add(skill.trim());
        }

        return normalizedSkills;
    }

    private List<Address> toAddressEntities(List<AddressRequest> addressRequests) {
        List<Address> addresses = new ArrayList<>();
        if (addressRequests == null) {
            return addresses;
        }

        for (AddressRequest addressRequest : addressRequests) {
            Address address = new Address();
            address.setHouseNo(addressRequest.houseNo().trim());
            address.setStreet(addressRequest.street().trim());
            address.setCity(addressRequest.city().trim());
            address.setState(addressRequest.state().trim());
            address.setPincode(addressRequest.pincode());
            addresses.add(address);
        }

        return addresses;
    }

    private ProfileResponse toResponse(UserProfile profile) {
        List<AddressResponse> addressResponses = profile.getAddresses().stream()
            .map(address -> new AddressResponse(
                address.getAddressId(),
                address.getHouseNo(),
                address.getStreet(),
                address.getCity(),
                address.getState(),
                address.getPincode()
            ))
            .toList();

        if (profile instanceof CandidateProfile candidateProfile) {
            return new ProfileResponse(
                candidateProfile.getProfileId(),
                candidateProfile.getRole(),
                candidateProfile.getFullName(),
                candidateProfile.getEmail(),
                candidateProfile.getMobile(),
                candidateProfile.getDob(),
                candidateProfile.getGender(),
                List.copyOf(candidateProfile.getSkills()),
                candidateProfile.getExperience(),
                candidateProfile.getResumeUrl(),
                null,
                null,
                null,
                null,
                addressResponses
            );
        }

        RecruiterProfile recruiterProfile = (RecruiterProfile) profile;
        return new ProfileResponse(
            recruiterProfile.getProfileId(),
            recruiterProfile.getRole(),
            recruiterProfile.getFullName(),
            recruiterProfile.getEmail(),
            recruiterProfile.getMobile(),
            recruiterProfile.getDob(),
            recruiterProfile.getGender(),
            List.of(),
            null,
            null,
            recruiterProfile.getCompanyName(),
            recruiterProfile.getCompanySize(),
            recruiterProfile.getIndustry(),
            recruiterProfile.getWebsite(),
            addressResponses
        );
    }
}
