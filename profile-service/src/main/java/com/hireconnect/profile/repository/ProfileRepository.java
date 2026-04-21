package com.hireconnect.profile.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.hireconnect.profile.domain.UserProfile;

public interface ProfileRepository extends JpaRepository<UserProfile, Integer> {

    @EntityGraph(attributePaths = {"addresses"})
    Optional<UserProfile> findByEmail(String email);

    @EntityGraph(attributePaths = {"addresses"})
    Optional<UserProfile> findByMobile(Long mobile);

    @EntityGraph(attributePaths = {"addresses"})
    List<UserProfile> findAllByRole(String role);

    @EntityGraph(attributePaths = {"addresses"})
    Optional<UserProfile> findByProfileId(Integer profileId);

    void deleteByProfileId(Integer profileId);

    boolean existsByEmail(String email);

    boolean existsByMobile(Long mobile);
}
