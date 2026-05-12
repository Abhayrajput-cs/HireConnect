package com.hireconnect.application.client;

import java.util.List;

public interface InterviewCatalogClient {

    List<InterviewSnapshot> getByApplication(Integer applicationId);
}
