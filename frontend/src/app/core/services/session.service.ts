import { computed, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';

import { UserRole } from '../constants/role.constants';
import { SessionSnapshot, UserSummary } from '../models/auth.models';
import { StorageService } from './storage.service';

@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly sessionSignal = signal<SessionSnapshot | null>(null);

  readonly session = computed(() => this.sessionSignal());
  readonly user = computed<UserSummary | null>(() => this.sessionSignal()?.user ?? null);
  readonly accessToken = computed(() => this.sessionSignal()?.accessToken ?? null);
  readonly refreshToken = computed(() => this.sessionSignal()?.refreshToken ?? null);
  readonly isAuthenticated = computed(() => !!this.sessionSignal()?.accessToken);
  readonly role = computed<UserRole | null>(() => this.user()?.role ?? null);

  constructor(
    private readonly storage: StorageService,
    private readonly router: Router,
  ) {
    this.sessionSignal.set(this.storage.getSnapshot());
  }

  setSession(snapshot: SessionSnapshot): void {
    this.sessionSignal.set(snapshot);
    this.storage.saveSnapshot(snapshot);
  }

  setUser(user: UserSummary): void {
    const snapshot = this.sessionSignal();
    if (!snapshot) {
      return;
    }

    this.setSession({ ...snapshot, user });
  }

  clear(redirect = true): void {
    this.sessionSignal.set(null);
    this.storage.clear();
    if (redirect) {
      void this.router.navigate(['/login']);
    }
  }

  roleHome(role = this.role()): string {
    switch (role) {
      case 'RECRUITER':
        return '/recruiter/dashboard';
      case 'ADMIN':
        return '/admin/dashboard';
      case 'CANDIDATE':
      default:
        return '/candidate/dashboard';
    }
  }
}
