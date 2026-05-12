import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_ENDPOINTS } from '../constants/api.constants';
import { JobQuery, JobRequest, JobResponse } from '../models/job.models';

@Injectable({ providedIn: 'root' })
export class JobService {
  private readonly http = inject(HttpClient);

  getJobs(query: JobQuery = {}): Observable<JobResponse[]> {
    let params = new HttpParams();
    Object.entries(query).forEach(([key, value]) => {
      if (value !== null && value !== undefined && value !== '') {
        params = params.set(key, String(value));
      }
    });
    return this.http.get<JobResponse[]>(API_ENDPOINTS.jobs, { params });
  }

  getJob(jobId: number): Observable<JobResponse> {
    return this.http.get<JobResponse>(`${API_ENDPOINTS.jobs}/${jobId}`);
  }

  getJobsByRecruiter(profileId: number): Observable<JobResponse[]> {
    return this.http.get<JobResponse[]>(`${API_ENDPOINTS.jobs}/recruiter/${profileId}`);
  }

  createJob(payload: JobRequest): Observable<JobResponse> {
    return this.http.post<JobResponse>(API_ENDPOINTS.jobs, payload);
  }

  updateJob(jobId: number, payload: Partial<JobRequest>): Observable<JobResponse> {
    return this.http.put<JobResponse>(`${API_ENDPOINTS.jobs}/${jobId}`, payload);
  }

  deleteJob(jobId: number): Observable<void> {
    return this.http.delete<void>(`${API_ENDPOINTS.jobs}/${jobId}`);
  }
}
