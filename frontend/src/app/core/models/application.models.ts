export interface ApplicationRequest {
  jobId: number;
  candidateId: number;
  coverLetter?: string | null;
  resumeUrl: string;
}

export interface ApplicationResponse {
  applicationId: number;
  jobId: number;
  candidateId: number;
  appliedAt: string;
  status: string;
  coverLetter: string | null;
  resumeUrl: string;
}

export interface StatusUpdateRequest {
  status: string;
}
