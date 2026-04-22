package com.hireconnect.web.dto;

import jakarta.validation.constraints.NotBlank;

public class SubscriptionSelectionForm {

    @NotBlank
    private String planCode;

    private boolean autoRenew = true;

    public String getPlanCode() {
        return planCode;
    }

    public void setPlanCode(String planCode) {
        this.planCode = planCode;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    public void setAutoRenew(boolean autoRenew) {
        this.autoRenew = autoRenew;
    }
}
