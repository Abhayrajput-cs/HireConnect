import { Injectable, inject } from '@angular/core';
import { Observable, catchError, finalize, of, shareReplay, tap } from 'rxjs';

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
      this.clearCache();
      return of(null);
    }

    if (!force && this.cacheEmail === email && this.cachedProfile) {
      return of(this.cachedProfile);
    }

    if (!force && this.cacheEmail === email && this.inFlight$) {
      return this.inFlight$;
    }

    const requestedEmail = email.toLowerCase();
    this.cacheEmail = requestedEmail;
    if (force) {
      this.cachedProfile = null;
    }

    this.inFlight$ = this.profiles.getProfileByEmail(email).pipe(
      catchError(() => of(null)),
      tap((profile) => {
        const currentEmail = this.session.user()?.email?.toLowerCase() ?? null;
        if (currentEmail === requestedEmail && this.cacheEmail === requestedEmail) {
          this.cachedProfile = profile;
        }
      }),
      finalize(() => {
        if (this.cacheEmail === requestedEmail) {
          this.inFlight$ = null;
        }
      }),
      shareReplay(1),
    );

    return this.inFlight$;
  }

  setCurrentProfile(profile: ProfileResponse | null): void {
    this.cacheEmail = (profile?.email ?? this.session.user()?.email ?? null)?.toLowerCase() ?? null;
    this.cachedProfile = profile;
    this.inFlight$ = null;
  }

  clearCache(): void {
    this.cacheEmail = null;
    this.cachedProfile = null;
    this.inFlight$ = null;
  }

}
