package com.hireconnect.interview.client;

public interface ApplicationCatalogClient {

    ApplicationSnapshot getApplication(Integer applicationId);

    void markInterviewScheduled(Integer applicationId);
}
