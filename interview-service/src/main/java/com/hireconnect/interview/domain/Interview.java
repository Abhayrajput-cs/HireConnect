package com.hireconnect.interview.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "interviews")
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interview_id")
    private Integer interviewId;

    @Column(name = "application_id", nullable = false)
    private Integer applicationId;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(nullable = false, length = 30)
    private String mode;

    @Column(name = "meet_link", length = 500)
    private String meetLink;

    @Column(length = 255)
    private String location;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(length = 2000)
    private String notes;

    @Column(name = "requested_scheduled_at")
    private LocalDateTime requestedScheduledAt;

    @Column(name = "requested_meet_link", length = 500)
    private String requestedMeetLink;

    @Column(name = "requested_location", length = 255)
    private String requestedLocation;

    @Column(name = "requested_notes", length = 2000)
    private String requestedNotes;

    @Column(name = "status_before_reschedule", length = 40)
    private String statusBeforeReschedule;

    public Integer getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(Integer interviewId) {
        this.interviewId = interviewId;
    }

    public Integer getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Integer applicationId) {
        this.applicationId = applicationId;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getMeetLink() {
        return meetLink;
    }

    public void setMeetLink(String meetLink) {
        this.meetLink = meetLink;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getRequestedScheduledAt() {
        return requestedScheduledAt;
    }

    public void setRequestedScheduledAt(LocalDateTime requestedScheduledAt) {
        this.requestedScheduledAt = requestedScheduledAt;
    }

    public String getRequestedMeetLink() {
        return requestedMeetLink;
    }

    public void setRequestedMeetLink(String requestedMeetLink) {
        this.requestedMeetLink = requestedMeetLink;
    }

    public String getRequestedLocation() {
        return requestedLocation;
    }

    public void setRequestedLocation(String requestedLocation) {
        this.requestedLocation = requestedLocation;
    }

    public String getRequestedNotes() {
        return requestedNotes;
    }

    public void setRequestedNotes(String requestedNotes) {
        this.requestedNotes = requestedNotes;
    }

    public String getStatusBeforeReschedule() {
        return statusBeforeReschedule;
    }

    public void setStatusBeforeReschedule(String statusBeforeReschedule) {
        this.statusBeforeReschedule = statusBeforeReschedule;
    }
}
