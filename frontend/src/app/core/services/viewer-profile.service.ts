import { Injectable, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, finalize, map, of, shareReplay, tap } from 'rxjs';

import { ProfileResponse } from '../models/profile.models';
import { ProfileService } from './profile.service';
import { SessionService } from './session.service';

@Injectable({ providedIn: 'root' })
export class ViewerProfileService {
  private static readonly PROFILE_STORAGE_PREFIX = 'hireconnect.profile.';

  private readonly profiles = inject(ProfileService);
  private readonly session = inject(SessionService);

  private cacheEmail: string | null = null;
  private cachedProfile: ProfileResponse | null = null;
  private inFlight$: Observable<ProfileResponse | null> | null = null;

  getCurrentProfile(force = false): Observable<ProfileResponse | null> {
    const email = this.session.user()?.email ?? null;
    if (!email) {
      return of(null);
    }

    if (!force && this.cacheEmail === email && this.cachedProfile) {
      return of(this.cachedProfile);
    }

    if (!force && this.cacheEmail === email && this.inFlight$) {
      return this.inFlight$;
    }

    this.cacheEmail = email;
    this.inFlight$ = this.profiles.getProfileByEmail(email).pipe(
      catchError((error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 404) {
          return of(this.readStoredProfile(email));
        }

        return this.profiles.getProfiles().pipe(
          map((profiles) => profiles.find((profile) => profile.email.toLowerCase() === email.toLowerCase()) ?? null),
          catchError(() => of(this.readStoredProfile(email))),
        );
      }),
      tap((profile) => {
        this.cachedProfile = profile;
        if (profile) {
          this.writeStoredProfile(profile);
        }
      }),
      finalize(() => {
        this.inFlight$ = null;
      }),
      shareReplay(1),
    );

    return this.inFlight$;
  }

  setCurrentProfile(profile: ProfileResponse | null): void {
    this.cacheEmail = profile?.email ?? this.session.user()?.email ?? null;
    this.cachedProfile = profile;
    this.inFlight$ = null;
    if (profile) {
      this.writeStoredProfile(profile);
    }
  }

  clearCache(): void {
    this.cacheEmail = null;
    this.cachedProfile = null;
    this.inFlight$ = null;
  }

  private writeStoredProfile(profile: ProfileResponse): void {
    if (typeof window === 'undefined') {
      return;
    }

    window.localStorage.setItem(
      this.profileStorageKey(profile.email),
      JSON.stringify(profile),
    );
  }

  private readStoredProfile(email: string): ProfileResponse | null {
    if (typeof window === 'undefined') {
      return null;
    }

    const raw = window.localStorage.getItem(this.profileStorageKey(email));
    if (!raw) {
      return null;
    }

    try {
      return JSON.parse(raw) as ProfileResponse;
    } catch {
      window.localStorage.removeItem(this.profileStorageKey(email));
      return null;
    }
  }

  private profileStorageKey(email: string): string {
    return `${ViewerProfileService.PROFILE_STORAGE_PREFIX}${email.toLowerCase()}`;
  }
}
