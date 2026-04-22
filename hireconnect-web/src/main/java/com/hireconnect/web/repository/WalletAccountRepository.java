package com.hireconnect.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hireconnect.web.domain.WalletAccount;

public interface WalletAccountRepository extends JpaRepository<WalletAccount, Integer> {
}
