import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_ENDPOINTS } from '../constants/api.constants';
import { AnalyticsSummary } from '../models/analytics.models';
import { JobViewRequest } from '../models/notification.models';

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private readonly http = inject(HttpClient);

  recordJobView(jobId: number, payload: JobViewRequest = {}): Observable<number> {
    return this.http.post<number>(`${API_ENDPOINTS.analytics}/jobs/${jobId}/views`, payload);
  }

  getJobViewCount(jobId: number): Observable<number> {
    return this.http.get<number>(`${API_ENDPOINTS.analytics}/jobs/${jobId}/view-count`);
  }

  getApplicationCount(jobId: number): Observable<number> {
    return this.http.get<number>(`${API_ENDPOINTS.analytics}/jobs/${jobId}/application-count`);
  }

  getViewToApplyRatio(jobId: number): Observable<number> {
    return this.http.get<number>(`${API_ENDPOINTS.analytics}/jobs/${jobId}/view-to-apply-ratio`);
  }

  getRecruiterStats(recruiterId: number): Observable<AnalyticsSummary> {
    return this.http.get<AnalyticsSummary>(`${API_ENDPOINTS.analytics}/recruiter/${recruiterId}`);
  }

  getTimeToHire(recruiterId: number): Observable<number> {
    return this.http.get<number>(`${API_ENDPOINTS.analytics}/recruiter/${recruiterId}/time-to-hire`);
  }

  getPlatformStats(): Observable<AnalyticsSummary> {
    return this.http.get<AnalyticsSummary>(`${API_ENDPOINTS.analytics}/admin`);
  }

  getTopCategories(): Observable<Record<string, number>> {
    return this.http.get<Record<string, number>>(`${API_ENDPOINTS.analytics}/categories/top`);
  }
}
