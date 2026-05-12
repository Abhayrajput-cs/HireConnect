import { environment } from '../../../environments/environment';

const gateway = environment.gatewayUrl;

export const API_ENDPOINTS = {
  auth: `${gateway}/api/auth`,
  profile: `${gateway}/api/v1/profiles`,
  jobs: `${gateway}/api/v1/jobs`,
  applications: `${gateway}/api/v1/applications`,
  interviews: `${gateway}/api/v1/interviews`,
  notifications: `${gateway}/api/v1/notifications`,
  analytics: `${gateway}/api/v1/analytics`,
  payments: `${gateway}/api/v1/payments`,
  githubOAuthStart: `${environment.authDirectUrl}/oauth2/authorization/github`,
} as const;
