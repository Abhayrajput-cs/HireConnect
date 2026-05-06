export interface InterviewScheduleRequest {
  applicationId: number;
  scheduledAt: string;
  mode: string;
  meetLink?: string | null;
  location?: string | null;
  notes?: string | null;
}

export interface InterviewRescheduleRequest {
  scheduledAt: string;
  meetLink?: string | null;
  location?: string | null;
  notes?: string | null;
}

export interface InterviewResponse {
  interviewId: number;
  applicationId: number;
  scheduledAt: string;
  mode: string;
  meetLink: string | null;
  location: string | null;
  status: string;
  notes: string | null;
  requestedScheduledAt?: string | null;
  requestedMeetLink?: string | null;
  requestedLocation?: string | null;
  requestedNotes?: string | null;
}
