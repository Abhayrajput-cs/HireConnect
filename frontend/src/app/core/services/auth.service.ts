import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, map, of, shareReplay, switchMap, tap, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { API_ENDPOINTS } from '../constants/api.constants';
import { UserRole } from '../constants/role.constants';
import {
  AuthResponse,
  LoginRequest,
  RefreshTokenRequest,
  RegisterRequest,
  SessionSnapshot,
  TokenValidationResponse,
  UserSummary,
} from '../models/auth.models';
import { SessionService } from './session.service';
import { StorageService } from './storage.service';
import { ToastService } from './toast.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly session = inject(SessionService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly storage = inject(StorageService);

  private refreshRequest$: Observable<AuthResponse> | null = null;

  bootstrapSession(): Promise<void> {
    const snapshot = this.storage.getSnapshot();
    if (!snapshot) {
      return Promise.resolve();
    }

    this.session.setSession(snapshot);
    return new Promise((resolve) => {
      this.me()
        .pipe(
          tap((user) => this.session.setUser(user)),
          catchError(() =>
            this.refreshAccessToken().pipe(
              switchMap(() => this.me()),
              tap((user) => this.session.setUser(user)),
            ),
          ),
          catchError(() => {
            this.session.clear(false);
            return of(null);
          }),
        )
        .subscribe({ complete: () => resolve() });
    });
  }

  login(payload: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_ENDPOINTS.auth}/login`, payload).pipe(
      tap((response) => this.persistResponse(response)),
    );
  }

  register(payload: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_ENDPOINTS.auth}/register`, payload).pipe(
      tap((response) => this.persistResponse(response)),
    );
  }

  logout(): Observable<void> {
    return this.http.post(`${API_ENDPOINTS.auth}/logout`, {}, { responseType: 'text' }).pipe(
      map(() => void 0),
      finalize(() => this.session.clear(false)),
      tap(() => {
        this.toast.success('Signed out', 'Your session has been closed securely.');
        void this.router.navigate(['/login']);
      }),
      catchError((error) => {
        this.session.clear(false);
        void this.router.navigate(['/login']);
        return throwError(() => error);
      }),
    );
  }

  refreshAccessToken(): Observable<AuthResponse> {
    if (this.refreshRequest$) {
      return this.refreshRequest$;
    }

    const refreshToken = this.session.refreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token available'));
    }

    const payload: RefreshTokenRequest = { refreshToken };
    this.refreshRequest$ = this.http.post<AuthResponse>(`${API_ENDPOINTS.auth}/refresh`, payload).pipe(
      tap((response) => this.persistResponse(response)),
      finalize(() => {
        this.refreshRequest$ = null;
      }),
      shareReplay(1),
    );

    return this.refreshRequest$;
  }

  validate(token: string): Observable<TokenValidationResponse> {
    return this.http.post<TokenValidationResponse>(`${API_ENDPOINTS.auth}/validate`, { token });
  }

  me(): Observable<UserSummary> {
    return this.http.get<UserSummary>(`${API_ENDPOINTS.auth}/me`);
  }

  completeOAuthCallback(params: URLSearchParams): Observable<UserSummary | null> {
    const accessToken = params.get('accessToken');
    const refreshToken = params.get('refreshToken');
    const email = params.get('email');
    const role = params.get('role') as UserRole | null;

    if (!accessToken || !refreshToken || !email || !role) {
      return of(null);
    }

    const snapshot: SessionSnapshot = {
      accessToken,
      refreshToken,
      user: {
        userId: -1,
        email,
        role,
        provider: 'GITHUB',
        createdAt: new Date().toISOString(),
      },
    };

    this.session.setSession(snapshot);
    return this.me().pipe(
      tap((user) => this.session.setUser(user)),
      catchError(() => of(snapshot.user)),
    );
  }

  buildGithubLoginUrl(role: Exclude<UserRole, 'ADMIN'>): string {
    const query = new URLSearchParams({
      role,
      redirect_uri: environment.oauthCallbackUrl,
    });
    return `${API_ENDPOINTS.githubOAuthStart}?${query.toString()}`;
  }

  redirectToRoleHome(role = this.session.role()): void {
    void this.router.navigateByUrl(this.session.roleHome(role));
  }

  private persistResponse(response: AuthResponse): void {
    this.session.setSession({
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      user: response.user,
    });
  }
}
