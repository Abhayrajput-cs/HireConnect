import { HttpErrorResponse } from '@angular/common/http';

export function getErrorMessage(error: unknown, fallback = 'Something went wrong. Please try again.'): string {
  if (!(error instanceof HttpErrorResponse)) {
    return fallback;
  }

  if (error.status === 0) {
    return 'We could not reach HireConnect right now. Please make sure the backend services are running and try again.';
  }

  const payload = error.error;
  if (typeof payload === 'string' && payload.trim()) {
    if (looksLikeRawHttpError(payload)) {
      return friendlyStatusMessage(error.status, fallback);
    }
    return payload;
  }

  if (payload && typeof payload === 'object') {
    const validationErrors = (payload as { validationErrors?: unknown }).validationErrors;
    if (validationErrors && typeof validationErrors === 'object') {
      const details = Object.entries(validationErrors as Record<string, unknown>)
        .map(([field, value]) => `${field}: ${String(value)}`)
        .join(', ');
      if (details.trim()) {
        return details;
      }
    }

    const message = (payload as { message?: unknown }).message;
    if (typeof message === 'string' && message.trim()) {
      if (looksLikeRawHttpError(message)) {
        return friendlyStatusMessage(error.status, fallback);
      }
      return message;
    }
  }

  return friendlyStatusMessage(error.status, fallback);
}

function friendlyStatusMessage(status: number, fallback: string): string {
  if (status === 400) {
    return 'Some details need attention. Please review the form and try again.';
  }
  if (status === 401) {
    return 'Your session has expired or the login details are incorrect. Please sign in again.';
  }
  if (status === 402) {
    return 'This action requires an active subscription. Please upgrade your plan to continue.';
  }
  if (status === 403) {
    return 'You do not have permission to perform this action.';
  }
  if (status === 404) {
    return 'We could not find the requested resource. Please refresh and try again.';
  }
  if (status === 409) {
    return 'This record already exists or conflicts with existing data.';
  }
  if (status === 502 || status === 503 || status === 504) {
    return 'A required HireConnect service is temporarily unavailable. Please try again in a moment.';
  }
  if (status >= 500) {
    return 'Something went wrong on our side. Please try again in a moment.';
  }
  return fallback;
}

function looksLikeRawHttpError(message: string): boolean {
  return /Http failure response|Unknown Error|status code|Bad Gateway|Internal Server Error/i.test(message);
}
