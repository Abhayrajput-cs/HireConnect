import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';

import { API_ENDPOINTS } from '../constants/api.constants';
import { AuthService } from '../services/auth.service';
import { SessionService } from '../services/session.service';
import { ToastService } from '../services/toast.service';

let loginRedirectInProgress = false;

function isPublicAuthRequest(url: string): boolean {
  return url.includes('/api/auth/login')
    || url.includes('/api/auth/register')
    || url.includes('/api/auth/refresh')
    || url.includes('/api/auth/validate');
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const session = inject(SessionService);
  const auth = inject(AuthService);
  const router = inject(Router);
  const toast = inject(ToastService);
  const accessToken = session.accessToken();

  const authenticatedRequest = accessToken && !isPublicAuthRequest(req.url)
    ? req.clone({
        setHeaders: {
          Authorization: `Bearer ${accessToken}`,
        },
      })
    : req;

  return next(authenticatedRequest).pipe(
    catchError((error: HttpErrorResponse) => {
      const refreshToken = session.refreshToken();
      const shouldRefresh = error.status === 401
        && !!refreshToken
        && !isPublicAuthRequest(req.url)
        && !req.url.startsWith(API_ENDPOINTS.githubOAuthStart);

      if (!shouldRefresh) {
        if (error.status === 401 && !req.url.includes('/api/auth/login')) {
          session.clear(false);
          if (!loginRedirectInProgress && router.url !== '/login') {
            loginRedirectInProgress = true;
            toast.info('Please sign in again', 'Your session has expired. Sign in again to continue securely.');
            void router.navigate(['/login']).finally(() => {
              window.setTimeout(() => {
                loginRedirectInProgress = false;
              }, 1000);
            });
          }
        }
        return throwError(() => error);
      }

      return auth.refreshAccessToken().pipe(
        switchMap((response) =>
          next(
            req.clone({
              setHeaders: {
                Authorization: `Bearer ${response.accessToken}`,
              },
            }),
          ),
        ),
        catchError((refreshError) => {
          session.clear(false);
          if (!loginRedirectInProgress && router.url !== '/login') {
            loginRedirectInProgress = true;
            toast.info('Please sign in again', 'Your session could not be restored. Sign in again to continue.');
            void router.navigate(['/login']).finally(() => {
              window.setTimeout(() => {
                loginRedirectInProgress = false;
              }, 1000);
            });
          }
          return throwError(() => refreshError);
        }),
      );
    }),
  );
};
