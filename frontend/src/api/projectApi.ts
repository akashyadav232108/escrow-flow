import api from './client';
import type { CreateProjectInput, Project } from '../types';

export const projectApi = {
  getProjects: (status?: string) =>
    api
      .get<Project[]>('/projects', { params: status ? { status } : undefined })
      .then((res) => res.data),

  getProjectById: (id: number) =>
    api.get<Project>(`/projects/${id}`).then((res) => res.data),

  createProject: (payload: CreateProjectInput) =>
    api.post<Project>('/projects', payload).then((res) => res.data),

  acceptProject: (id: number) =>
    api.post<Project>(`/projects/${id}/accept`).then((res) => res.data),

  lockFunds: (milestoneId: number, idempotencyKey: string) =>
    api
      .post(`/milestones/${milestoneId}/lock-funds`, null, {
        headers: { 'Idempotency-Key': idempotencyKey },
      })
      .then((res) => res.data),

  submitMilestone: (milestoneId: number, note: string) =>
    api.post(`/milestones/${milestoneId}/submit`, { note }).then((res) => res.data),

  approveMilestone: (milestoneId: number) =>
    api.post(`/milestones/${milestoneId}/approve`).then((res) => res.data),

  disputeMilestone: (milestoneId: number, reason?: string) =>
    api
      .post(`/milestones/${milestoneId}/dispute`, reason ? { reason } : {})
      .then((res) => res.data),
};
