package com.hireconnect.profile.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "recruiter_profiles")
public class RecruiterProfile extends UserProfile {

    @Column(nullable = false, length = 120)
    private String companyName;

    @Column(length = 40)
    private String companySize;

    @Column(nullable = false, length = 80)
    private String industry;

    @Column(length = 255)
    private String website;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanySize() {
        return companySize;
    }

    public void setCompanySize(String companySize) {
        this.companySize = companySize;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }
}
