import api from './client';
import type {
  AdminDashboardStats,
  AdminUser,
  DisputeDetail,
  DisputeResolution,
  DisputeStatus,
  ManagedUser,
  AccountStatus,
} from '../types';

export const adminApi = {
  getDashboard: () =>
    api.get<AdminDashboardStats>('/admin/dashboard').then((res) => res.data),

  listAdmins: () => api.get<AdminUser[]>('/admin/admins').then((res) => res.data),

  createAdmin: (payload: { name: string; email: string; password: string }) =>
    api.post<AdminUser>('/admin/admins', payload).then((res) => res.data),

  listDisputes: (status?: DisputeStatus) =>
    api
      .get<DisputeDetail[]>('/admin/disputes', { params: status ? { status } : undefined })
      .then((res) => res.data),

  getDispute: (id: number) =>
    api.get<DisputeDetail>(`/admin/disputes/${id}`).then((res) => res.data),

  resolveDispute: (id: number, decision: DisputeResolution, note?: string) =>
    api
      .post<DisputeDetail>(`/admin/disputes/${id}/resolve`, { decision, note })
      .then((res) => res.data),

  listUsers: (status?: AccountStatus) =>
    api
      .get<ManagedUser[]>('/admin/users', { params: status ? { status } : undefined })
      .then((res) => res.data),

  getUser: (id: number) =>
    api.get<ManagedUser>(`/admin/users/${id}`).then((res) => res.data),

  warnUser: (id: number, reason: string) =>
    api.post<ManagedUser>(`/admin/users/${id}/warnings`, { reason }).then((res) => res.data),

  suspendUser: (id: number, reason: string) =>
    api.post<ManagedUser>(`/admin/users/${id}/suspend`, { reason }).then((res) => res.data),

  unsuspendUser: (id: number) =>
    api.post<ManagedUser>(`/admin/users/${id}/unsuspend`).then((res) => res.data),

  deleteUser: (id: number, reason: string) =>
    api.post<ManagedUser>(`/admin/users/${id}/delete`, { reason }).then((res) => res.data),
};
