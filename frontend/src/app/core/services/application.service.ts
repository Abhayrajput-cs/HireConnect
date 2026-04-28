import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_ENDPOINTS } from '../constants/api.constants';
import { ApplicationRequest, ApplicationResponse, StatusUpdateRequest } from '../models/application.models';

@Injectable({ providedIn: 'root' })
export class ApplicationService {
  private readonly http = inject(HttpClient);

  submit(payload: ApplicationRequest): Observable<ApplicationResponse> {
    return this.http.post<ApplicationResponse>(API_ENDPOINTS.applications, payload);
  }

  getById(applicationId: number): Observable<ApplicationResponse> {
    return this.http.get<ApplicationResponse>(`${API_ENDPOINTS.applications}/${applicationId}`);
  }

  getByCandidate(candidateId: number): Observable<ApplicationResponse[]> {
    return this.http.get<ApplicationResponse[]>(`${API_ENDPOINTS.applications}/candidate/${candidateId}`);
  }

  getByJob(jobId: number): Observable<ApplicationResponse[]> {
    return this.http.get<ApplicationResponse[]>(`${API_ENDPOINTS.applications}/job/${jobId}`);
  }

  countByJob(jobId: number): Observable<number> {
    return this.http.get<number>(`${API_ENDPOINTS.applications}/job/${jobId}/count`);
  }

  searchByStatus(status: string): Observable<ApplicationResponse[]> {
    return this.http.get<ApplicationResponse[]>(API_ENDPOINTS.applications, {
      params: new HttpParams().set('status', status),
    });
  }

  searchByDateRange(appliedFrom: string, appliedTo: string): Observable<ApplicationResponse[]> {
    return this.http.get<ApplicationResponse[]>(API_ENDPOINTS.applications, {
      params: new HttpParams().set('appliedFrom', appliedFrom).set('appliedTo', appliedTo),
    });
  }

  updateStatus(applicationId: number, payload: StatusUpdateRequest): Observable<ApplicationResponse> {
    return this.http.patch<ApplicationResponse>(`${API_ENDPOINTS.applications}/${applicationId}/status`, payload);
  }

  withdraw(applicationId: number): Observable<ApplicationResponse> {
    return this.http.patch<ApplicationResponse>(`${API_ENDPOINTS.applications}/${applicationId}/withdraw`, {});
  }
}
