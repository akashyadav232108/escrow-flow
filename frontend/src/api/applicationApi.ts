import api from './client';
import type { ApplyToProjectInput, ProjectApplication } from '../types';

export const applicationApi = {
  apply: (projectId: number, payload: ApplyToProjectInput = {}) =>
    api
      .post<ProjectApplication>(`/projects/${projectId}/applications`, payload)
      .then((res) => res.data),

  listForProject: (projectId: number) =>
    api.get<ProjectApplication[]>(`/projects/${projectId}/applications`).then((res) => res.data),

  listMine: () => api.get<ProjectApplication[]>('/applications/mine').then((res) => res.data),

  accept: (applicationId: number) =>
    api.post<ProjectApplication>(`/applications/${applicationId}/accept`).then((res) => res.data),

  decline: (applicationId: number) =>
    api.post<ProjectApplication>(`/applications/${applicationId}/decline`).then((res) => res.data),

  withdraw: (applicationId: number) =>
    api.post<ProjectApplication>(`/applications/${applicationId}/withdraw`).then((res) => res.data),
};
