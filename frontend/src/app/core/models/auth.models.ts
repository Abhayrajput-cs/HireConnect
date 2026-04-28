import { UserRole } from '../constants/role.constants';

export interface UserSummary {
  userId: number;
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

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  role: Exclude<UserRole, 'ADMIN'>;
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
