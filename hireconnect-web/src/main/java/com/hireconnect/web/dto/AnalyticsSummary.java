package com.hireconnect.web.dto;

public class AnalyticsSummary {

    private int totalJobs;
    private int totalApplications;
    private int shortlistedCount;
    private int offeredCount;
    private int rejectedCount;
    private double avgTimeToHireDays;
    private double viewToApplyRatio;

    public int getTotalJobs() {
        return totalJobs;
    }

    public void setTotalJobs(int totalJobs) {
        this.totalJobs = totalJobs;
    }

    public int getTotalApplications() {
        return totalApplications;
    }

    public void setTotalApplications(int totalApplications) {
        this.totalApplications = totalApplications;
    }

    public int getShortlistedCount() {
        return shortlistedCount;
    }

    public void setShortlistedCount(int shortlistedCount) {
        this.shortlistedCount = shortlistedCount;
    }

    public int getOfferedCount() {
        return offeredCount;
    }

    public void setOfferedCount(int offeredCount) {
        this.offeredCount = offeredCount;
    }

    public int getRejectedCount() {
        return rejectedCount;
    }

    public void setRejectedCount(int rejectedCount) {
        this.rejectedCount = rejectedCount;
    }

    public double getAvgTimeToHireDays() {
        return avgTimeToHireDays;
    }

    public void setAvgTimeToHireDays(double avgTimeToHireDays) {
        this.avgTimeToHireDays = avgTimeToHireDays;
    }

    public double getViewToApplyRatio() {
        return viewToApplyRatio;
    }

    public void setViewToApplyRatio(double viewToApplyRatio) {
        this.viewToApplyRatio = viewToApplyRatio;
    }
}
