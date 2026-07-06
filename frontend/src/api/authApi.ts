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

export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
}

export const authApi = {
  signup: (payload: SignupPayload) =>
    api.post<AuthResponse>('/auth/signup', payload).then((res) => res.data),

  login: (payload: LoginPayload) =>
    api.post<AuthResponse>('/auth/login', payload).then((res) => res.data),

  changePassword: (payload: ChangePasswordPayload) =>
    api.post<void>('/auth/change-password', payload).then((res) => res.data),
};
