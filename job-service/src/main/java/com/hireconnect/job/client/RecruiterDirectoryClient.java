package com.hireconnect.job.client;

import java.util.List;

public interface RecruiterDirectoryClient {

    RecruiterProfileSnapshot getRecruiterProfile(Integer profileId);

    List<RecruiterProfileSnapshot> getProfilesByRole(String role);
}
