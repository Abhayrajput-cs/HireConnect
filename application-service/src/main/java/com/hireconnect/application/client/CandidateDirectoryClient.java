package com.hireconnect.application.client;

public interface CandidateDirectoryClient {

    CandidateProfileSnapshot getCandidateProfile(Integer candidateId);

    CandidateProfileSnapshot getCandidateProfileByEmail(String email);
}
