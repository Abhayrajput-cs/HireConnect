import { Injectable } from '@angular/core';

import { SessionSnapshot, UserSummary } from '../models/auth.models';

const ACCESS_TOKEN_KEY = 'hireconnect.accessToken';
const REFRESH_TOKEN_KEY = 'hireconnect.refreshToken';
const USER_KEY = 'hireconnect.user';

@Injectable({ providedIn: 'root' })
export class StorageService {
  private get storage(): Storage | null {
    return typeof window === 'undefined' ? null : window.localStorage;
  }

  getSnapshot(): SessionSnapshot | null {
    const accessToken = this.getAccessToken();
    const refreshToken = this.getRefreshToken();
    const user = this.getUser();

    if (!accessToken || !refreshToken) {
      return null;
    }

    return { accessToken, refreshToken, user };
  }

  saveSnapshot(snapshot: SessionSnapshot): void {
    const storage = this.storage;
    if (!storage) {
      return;
    }

    storage.setItem(ACCESS_TOKEN_KEY, snapshot.accessToken);
    storage.setItem(REFRESH_TOKEN_KEY, snapshot.refreshToken);

    if (snapshot.user) {
      storage.setItem(USER_KEY, JSON.stringify(snapshot.user));
    } else {
      storage.removeItem(USER_KEY);
    }
  }

  clear(): void {
    const storage = this.storage;
    if (!storage) {
      return;
    }

    storage.removeItem(ACCESS_TOKEN_KEY);
    storage.removeItem(REFRESH_TOKEN_KEY);
    storage.removeItem(USER_KEY);
  }

  getAccessToken(): string | null {
    return this.storage?.getItem(ACCESS_TOKEN_KEY) ?? null;
  }

  getRefreshToken(): string | null {
    return this.storage?.getItem(REFRESH_TOKEN_KEY) ?? null;
  }

  getUser(): UserSummary | null {
    const raw = this.storage?.getItem(USER_KEY);
    if (!raw) {
      return null;
    }

    try {
      return JSON.parse(raw) as UserSummary;
    } catch {
      this.storage?.removeItem(USER_KEY);
      return null;
    }
  }
}
