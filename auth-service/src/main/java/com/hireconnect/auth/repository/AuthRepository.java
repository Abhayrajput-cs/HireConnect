package com.hireconnect.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hireconnect.auth.domain.UserCredential;

public interface AuthRepository extends JpaRepository<UserCredential, Integer> {

    Optional<UserCredential> findByEmail(String email);

    Optional<UserCredential> findByUserId(Integer userId);

    boolean existsByEmail(String email);

    void deleteByUserId(Integer userId);
}
