import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_ENDPOINTS } from '../constants/api.constants';
import {
  InterviewRescheduleRequest,
  InterviewResponse,
  InterviewScheduleRequest,
} from '../models/interview.models';

@Injectable({ providedIn: 'root' })
export class InterviewService {
  private readonly http = inject(HttpClient);

  schedule(payload: InterviewScheduleRequest): Observable<InterviewResponse> {
    return this.http.post<InterviewResponse>(API_ENDPOINTS.interviews, payload);
  }

  confirm(interviewId: number): Observable<string> {
    return this.http.patch(`${API_ENDPOINTS.interviews}/${interviewId}/confirm`, {}, { responseType: 'text' });
  }

  reschedule(interviewId: number, payload: InterviewRescheduleRequest): Observable<InterviewResponse> {
    return this.http.patch<InterviewResponse>(`${API_ENDPOINTS.interviews}/${interviewId}/reschedule`, payload);
  }

  cancel(interviewId: number): Observable<void> {
    return this.http.delete<void>(`${API_ENDPOINTS.interviews}/${interviewId}`);
  }

  getById(interviewId: number): Observable<InterviewResponse> {
    return this.http.get<InterviewResponse>(`${API_ENDPOINTS.interviews}/${interviewId}`);
  }

  getByApplication(applicationId: number): Observable<InterviewResponse[]> {
    return this.http.get<InterviewResponse[]>(`${API_ENDPOINTS.interviews}/application/${applicationId}`);
  }

  getByStatus(status: string): Observable<InterviewResponse[]> {
    return this.http.get<InterviewResponse[]>(`${API_ENDPOINTS.interviews}/status/${status}`);
  }

  getByRange(scheduledFrom: string, scheduledTo: string): Observable<InterviewResponse[]> {
    const params = new HttpParams().set('scheduledFrom', scheduledFrom).set('scheduledTo', scheduledTo);
    return this.http.get<InterviewResponse[]>(API_ENDPOINTS.interviews, { params });
  }
}
