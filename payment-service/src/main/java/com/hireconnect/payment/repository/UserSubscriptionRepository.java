package com.hireconnect.payment.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hireconnect.payment.domain.SubscriptionStatus;
import com.hireconnect.payment.domain.UserRole;
import com.hireconnect.payment.domain.UserSubscription;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    Optional<UserSubscription> findFirstByUserIdAndStatusOrderByExpiryDateDesc(Long userId, SubscriptionStatus status);

    Optional<UserSubscription> findFirstByUserIdAndRoleAndStatusAndExpiryDateAfterOrderByExpiryDateDesc(
        Long userId,
        UserRole role,
        SubscriptionStatus status,
        Instant now
    );

    List<UserSubscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);
}
