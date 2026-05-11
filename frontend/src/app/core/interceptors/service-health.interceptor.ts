import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { API_ENDPOINTS } from '../constants/api.constants';
import { ToastService } from '../services/toast.service';

const LAST_SHOWN = new Map<string, number>();
const DEDUPE_WINDOW_MS = 15000;

export const serviceHealthInterceptor: HttpInterceptorFn = (req, next) => {
  const toast = inject(ToastService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (isServerFailure(error)) {
        const service = serviceName(req.url);
        const key = `${service}:${error.status}`;
        const now = Date.now();
        const lastShown = LAST_SHOWN.get(key) ?? 0;
        if (now - lastShown > DEDUPE_WINDOW_MS) {
          LAST_SHOWN.set(key, now);
          const unavailable = isServiceUnavailable(error);
          toast.error(
            unavailable ? `${service} unavailable` : `${service} error`,
            unavailable
              ? `${service} is unreachable right now. Please try again in a moment.`
              : `${service} could not complete the request right now. Please try again in a moment.`,
          );
        }
      }
      return throwError(() => error);
    }),
  );
};

function isServiceUnavailable(error: HttpErrorResponse): boolean {
  return error.status === 0 || error.status === 502 || error.status === 503 || error.status === 504;
}

function isServerFailure(error: HttpErrorResponse): boolean {
  return isServiceUnavailable(error) || error.status >= 500;
}

function serviceName(url: string): string {
  if (url.startsWith(API_ENDPOINTS.auth) || url.includes('/api/auth/')) {
    return 'Auth service';
  }
  if (url.startsWith(API_ENDPOINTS.profile) || url.includes('/api/v1/profiles')) {
    return 'Profile service';
  }
  if (url.startsWith(API_ENDPOINTS.jobs) || url.includes('/api/v1/jobs')) {
    return 'Job service';
  }
  if (url.startsWith(API_ENDPOINTS.applications) || url.includes('/api/v1/applications')) {
    return 'Application service';
  }
  if (url.startsWith(API_ENDPOINTS.interviews) || url.includes('/api/v1/interviews')) {
    return 'Interview service';
  }
  if (url.startsWith(API_ENDPOINTS.notifications) || url.includes('/api/v1/notifications')) {
    return 'Notification service';
  }
  if (url.startsWith(API_ENDPOINTS.analytics) || url.includes('/api/v1/analytics')) {
    return 'Analytics service';
  }
  if (url.startsWith(API_ENDPOINTS.payments) || url.includes('/api/v1/payments')) {
    return 'Payment service';
  }
  return 'HireConnect server';
}
