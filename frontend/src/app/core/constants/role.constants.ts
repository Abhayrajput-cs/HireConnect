export const USER_ROLES = ['CANDIDATE', 'RECRUITER', 'ADMIN'] as const;

export type UserRole = (typeof USER_ROLES)[number];

export const ROLE_LABELS: Record<UserRole, string> = {
  CANDIDATE: 'Candidate',
  RECRUITER: 'Recruiter',
  ADMIN: 'Administrator',
};
