package com.hireconnect.interview.client;

public interface ProfileDirectoryClient {

    ProfileSnapshot getProfileByEmail(String email);

    ProfileSnapshot getProfileById(Integer profileId);
}
