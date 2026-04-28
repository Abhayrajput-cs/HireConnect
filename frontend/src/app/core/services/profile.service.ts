import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_ENDPOINTS } from '../constants/api.constants';
import {
  CandidateProfileRequest,
  ProfileResponse,
  RecruiterProfileRequest,
} from '../models/profile.models';

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly http = inject(HttpClient);

  createCandidateProfile(payload: CandidateProfileRequest): Observable<ProfileResponse> {
    return this.http.post<ProfileResponse>(`${API_ENDPOINTS.profile}/candidates`, payload);
  }

  createRecruiterProfile(payload: RecruiterProfileRequest): Observable<ProfileResponse> {
    return this.http.post<ProfileResponse>(`${API_ENDPOINTS.profile}/recruiters`, payload);
  }

  getProfiles(role?: string | null): Observable<ProfileResponse[]> {
    let params = new HttpParams();
    if (role) {
      params = params.set('role', role);
    }
    return this.http.get<ProfileResponse[]>(API_ENDPOINTS.profile, { params });
  }

  getProfileById(profileId: number): Observable<ProfileResponse> {
    return this.http.get<ProfileResponse>(`${API_ENDPOINTS.profile}/${profileId}`);
  }

  getProfileByEmail(email: string): Observable<ProfileResponse> {
    return this.http.get<ProfileResponse>(`${API_ENDPOINTS.profile}/email/${encodeURIComponent(email)}`);
  }

  updateProfile(profileId: number, payload: object): Observable<ProfileResponse> {
    return this.http.put<ProfileResponse>(`${API_ENDPOINTS.profile}/${profileId}`, payload);
  }

  deleteProfile(profileId: number): Observable<void> {
    return this.http.delete<void>(`${API_ENDPOINTS.profile}/${profileId}`);
  }
}
