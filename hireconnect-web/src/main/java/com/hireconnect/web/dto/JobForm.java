package com.hireconnect.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class JobForm {

    @NotBlank
    private String title;

    @NotBlank
    private String category;

    @NotBlank
    private String type;

    @NotBlank
    private String location;

    @NotNull
    @PositiveOrZero
    private Double salaryMin;

    @NotNull
    @PositiveOrZero
    private Double salaryMax;

    @NotBlank
    private String description;

    @NotBlank
    private String skills;

    @NotNull
    @PositiveOrZero
    private Integer experienceRequired;

    private String status = "OPEN";

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Double getSalaryMin() {
        return salaryMin;
    }

    public void setSalaryMin(Double salaryMin) {
        this.salaryMin = salaryMin;
    }

    public Double getSalaryMax() {
        return salaryMax;
    }

    public void setSalaryMax(Double salaryMax) {
        this.salaryMax = salaryMax;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public Integer getExperienceRequired() {
        return experienceRequired;
    }

    public void setExperienceRequired(Integer experienceRequired) {
        this.experienceRequired = experienceRequired;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
