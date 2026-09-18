import api from './client';
import type { AuthResponse, Role } from '../types';

export interface SignupPayload {
  name: string;
  email: string;
  password: string;
  role: Role;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface VerifyEmailPayload {
  email: string;
  otp: string;
}

export interface ResendOtpPayload {
  email: string;
}

export interface ForgotPasswordPayload {
  email: string;
}

export interface ResetPasswordPayload {
  email: string;
  otp: string;
  newPassword: string;
}

export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
}

export const authApi = {
  signup: (payload: SignupPayload) =>
    api.post<void>('/auth/signup', payload),

  verifyEmail: (payload: VerifyEmailPayload) =>
    api.post<AuthResponse>('/auth/verify-email', payload).then((res) => res.data),

  resendOtp: (payload: ResendOtpPayload) =>
    api.post<void>('/auth/resend-otp', payload),

  login: (payload: LoginPayload) =>
    api.post<AuthResponse>('/auth/login', payload).then((res) => res.data),

  forgotPassword: (payload: ForgotPasswordPayload) =>
    api.post<void>('/auth/forgot-password', payload),

  resetPassword: (payload: ResetPasswordPayload) =>
    api.post<void>('/auth/reset-password', payload),

  changePassword: (payload: ChangePasswordPayload) =>
    api.post<void>('/auth/change-password', payload).then((res) => res.data),
};
