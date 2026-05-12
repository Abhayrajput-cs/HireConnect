import { UserRole } from '../constants/role.constants';

export interface UserSummary {
  userId: number;
  fullName: string;
  email: string;
  role: UserRole;
  provider: string;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  accessTokenExpiresAt: string;
  refreshTokenExpiresAt: string;
  user: UserSummary;
}

export interface RegistrationResponse {
  email: string;
  role: Exclude<UserRole, 'ADMIN'>;
  verificationRequired: boolean;
  message: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  role: Exclude<UserRole, 'ADMIN'>;
}

export interface VerifyEmailRequest {
  email: string;
  code: string;
}

export interface ResendVerificationRequest {
  email: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  email: string;
  code: string;
  newPassword: string;
}

export interface MessageResponse {
  message: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface TokenValidationResponse {
  valid: boolean;
  userId: number | null;
  email: string | null;
  role: UserRole | null;
  provider: string | null;
  expiresAt: string | null;
  message: string;
}

export interface SessionSnapshot {
  accessToken: string;
  refreshToken: string;
  user: UserSummary | null;
}
