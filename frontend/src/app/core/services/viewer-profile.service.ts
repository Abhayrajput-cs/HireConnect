import { Injectable, inject } from '@angular/core';
import { Observable, catchError, finalize, map, of, shareReplay, tap } from 'rxjs';

import { ProfileResponse } from '../models/profile.models';
import { ProfileService } from './profile.service';
import { SessionService } from './session.service';

@Injectable({ providedIn: 'root' })
export class ViewerProfileService {
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
        return this.profiles.getProfiles().pipe(
          map((profiles) => profiles.find((profile) => profile.email.toLowerCase() === email.toLowerCase()) ?? null),
          catchError(() => {
            return of(null);
          }),
        );
      }),
      tap((profile) => {
        this.cachedProfile = profile;
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
  }

  clearCache(): void {
    this.cacheEmail = null;
    this.cachedProfile = null;
    this.inFlight$ = null;
  }

}
