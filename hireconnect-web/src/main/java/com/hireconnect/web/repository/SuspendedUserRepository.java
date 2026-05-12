package com.hireconnect.web.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hireconnect.web.domain.SuspendedUser;

public interface SuspendedUserRepository extends JpaRepository<SuspendedUser, Integer> {

    List<SuspendedUser> findByActiveTrueOrderBySuspendedAtDesc();
}
