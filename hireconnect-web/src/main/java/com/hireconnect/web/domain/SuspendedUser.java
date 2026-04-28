package com.hireconnect.web.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "suspended_users")
public class SuspendedUser {

    @Id
    @Column(name = "profile_id")
    private Integer profileId;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 30)
    private String role;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "suspended_at", nullable = false)
    private LocalDateTime suspendedAt;

    public Integer getProfileId() {
        return profileId;
    }

    public void setProfileId(Integer profileId) {
        this.profileId = profileId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getSuspendedAt() {
        return suspendedAt;
    }

    @PrePersist
    void onCreate() {
        if (suspendedAt == null) {
            suspendedAt = LocalDateTime.now();
        }
    }
}
